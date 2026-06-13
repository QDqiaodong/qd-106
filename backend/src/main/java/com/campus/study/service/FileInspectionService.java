package com.campus.study.service;

import com.campus.study.entity.FileInspectionRecord;
import com.campus.study.entity.Material;
import com.campus.study.enums.FileInspectionExceptionType;
import com.campus.study.enums.FileInspectionHandleStatus;
import com.campus.study.repository.FileInspectionRecordRepository;
import com.campus.study.repository.MaterialRepository;
import com.campus.study.util.FileUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FileInspectionService {

    private static final Logger logger = LoggerFactory.getLogger(FileInspectionService.class);

    private static final List<Integer> UNRESOLVED_STATUSES = Arrays.asList(
            FileInspectionHandleStatus.PENDING.getCode(),
            FileInspectionHandleStatus.PROCESSING.getCode()
    );

    private static final List<String> PREVIEWABLE_EXTENSIONS = Arrays.asList(
            "pdf", "txt", "jpg", "jpeg", "png", "gif"
    );

    private static final long MAX_PREVIEW_FILE_SIZE = 50 * 1024 * 1024L;

    @Resource
    private FileInspectionRecordRepository inspectionRecordRepository;

    @Resource
    private MaterialRepository materialRepository;

    @Resource
    private FileUtil fileUtil;

    @Value("${file.upload.path:/app/uploads}")
    private String uploadPath;

    public String generateBatchId() {
        return "INSPECT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Transactional
    public Map<String, Object> runFullInspection() {
        String batchId = generateBatchId();
        logger.info("开始全量文件巡检，批次号: {}", batchId);

        List<Material> allMaterials = materialRepository.findAll();
        logger.info("待巡检资料总数: {}", allMaterials.size());

        int totalChecked = 0;
        int exceptionCount = 0;
        List<FileInspectionExceptionType> detectedTypes = new ArrayList<>();

        for (Material material : allMaterials) {
            totalChecked++;
            List<FileInspectionExceptionType> exceptions = inspectSingleMaterial(material, batchId);
            if (!exceptions.isEmpty()) {
                exceptionCount += exceptions.size();
                for (FileInspectionExceptionType type : exceptions) {
                    if (!detectedTypes.contains(type)) {
                        detectedTypes.add(type);
                    }
                }
            }
        }

        logger.info("全量巡检完成，批次号: {}, 检查总数: {}, 发现异常: {} 条", batchId, totalChecked, exceptionCount);

        Map<String, Object> result = new HashMap<>();
        result.put("batchId", batchId);
        result.put("totalChecked", totalChecked);
        result.put("exceptionCount", exceptionCount);
        result.put("exceptionTypes", detectedTypes.stream()
                .map(t -> Map.of("code", t.getCode(), "name", t.getName()))
                .toList());
        return result;
    }

    @Transactional
    public Map<String, Object> runIncrementalInspection(int limit) {
        String batchId = generateBatchId();
        logger.info("开始增量文件巡检，批次号: {}, 巡检数量: {}", batchId, limit);

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "updatedAt"));
        List<Material> materials = materialRepository.findAll(pageable).getContent();

        int totalChecked = 0;
        int exceptionCount = 0;
        List<FileInspectionExceptionType> detectedTypes = new ArrayList<>();

        for (Material material : materials) {
            totalChecked++;
            List<FileInspectionExceptionType> exceptions = inspectSingleMaterial(material, batchId);
            if (!exceptions.isEmpty()) {
                exceptionCount += exceptions.size();
                for (FileInspectionExceptionType type : exceptions) {
                    if (!detectedTypes.contains(type)) {
                        detectedTypes.add(type);
                    }
                }
            }
        }

        logger.info("增量巡检完成，批次号: {}, 检查总数: {}, 发现异常: {} 条", batchId, totalChecked, exceptionCount);

        Map<String, Object> result = new HashMap<>();
        result.put("batchId", batchId);
        result.put("totalChecked", totalChecked);
        result.put("exceptionCount", exceptionCount);
        result.put("exceptionTypes", detectedTypes.stream()
                .map(t -> Map.of("code", t.getCode(), "name", t.getName()))
                .toList());
        return result;
    }

    @Transactional
    public List<FileInspectionExceptionType> inspectSingleMaterial(Long materialId) {
        Material material = materialRepository.findById(materialId).orElse(null);
        if (material == null) {
            return new ArrayList<>();
        }
        String batchId = generateBatchId();
        return inspectSingleMaterial(material, batchId);
    }

    private List<FileInspectionExceptionType> inspectSingleMaterial(Material material, String batchId) {
        List<FileInspectionExceptionType> exceptions = new ArrayList<>();
        Long materialId = material.getId();

        if (material.getStatus() == null || material.getStatus() != 1) {
            createOrUpdateRecord(material, FileInspectionExceptionType.MATERIAL_OFFLINE,
                    "资料状态: " + material.getStatus(), batchId);
            exceptions.add(FileInspectionExceptionType.MATERIAL_OFFLINE);
        }

        String fileUrl = material.getFileUrl();
        if (fileUrl == null || fileUrl.isEmpty()) {
            createOrUpdateRecord(material, FileInspectionExceptionType.FILE_URL_EMPTY,
                    "数据库记录中file_url为空", batchId);
            exceptions.add(FileInspectionExceptionType.FILE_URL_EMPTY);
            return exceptions;
        }

        String relativePath = fileUrl.startsWith("/uploads/") ? fileUrl.substring(9) : fileUrl;
        File file = new File(uploadPath, relativePath);

        if (!file.exists()) {
            createOrUpdateRecord(material, FileInspectionExceptionType.FILE_MISSING,
                    "文件路径不存在: " + file.getAbsolutePath(), batchId);
            exceptions.add(FileInspectionExceptionType.FILE_MISSING);
            return exceptions;
        }

        if (!file.canRead()) {
            createOrUpdateRecord(material, FileInspectionExceptionType.FILE_UNREADABLE,
                    "文件不可读，可能存在权限问题: " + file.getAbsolutePath(), batchId);
            exceptions.add(FileInspectionExceptionType.FILE_UNREADABLE);
            return exceptions;
        }

        long actualSize = file.length();
        Long expectedSize = material.getFileSize();

        if (actualSize == 0) {
            createOrUpdateRecord(material, FileInspectionExceptionType.FILE_EMPTY,
                    "文件大小为0字节", batchId);
            exceptions.add(FileInspectionExceptionType.FILE_EMPTY);
        }

        if (expectedSize != null && expectedSize > 0 && actualSize != expectedSize) {
            FileInspectionRecord record = createOrUpdateRecord(material, FileInspectionExceptionType.FILE_SIZE_MISMATCH,
                    "期望大小: " + expectedSize + "字节, 实际大小: " + actualSize + "字节", batchId);
            if (record != null) {
                record.setExpectedFileSize(expectedSize);
                record.setActualFileSize(actualSize);
                inspectionRecordRepository.save(record);
            }
            exceptions.add(FileInspectionExceptionType.FILE_SIZE_MISMATCH);
        }

        String extension = fileUtil.getFileExtension(file.getName());
        if (!fileUtil.isValidExtension(extension)) {
            createOrUpdateRecord(material, FileInspectionExceptionType.EXTENSION_INVALID,
                    "文件扩展名: " + extension + " 不在允许列表中", batchId);
            exceptions.add(FileInspectionExceptionType.EXTENSION_INVALID);
        }

        String expectedHash = material.getFileHash();
        if (expectedHash != null && !expectedHash.isEmpty() && actualSize > 0) {
            try {
                String actualHash = fileUtil.calculateFileHash(file);
                if (!expectedHash.equalsIgnoreCase(actualHash)) {
                    FileInspectionRecord record = createOrUpdateRecord(material,
                            FileInspectionExceptionType.FILE_HASH_MISMATCH,
                            "文件哈希不匹配，文件可能已损坏或被篡改", batchId);
                    if (record != null) {
                        record.setExpectedFileHash(expectedHash);
                        record.setActualFileHash(actualHash);
                        record.setActualFileSize(actualSize);
                        inspectionRecordRepository.save(record);
                    }
                    exceptions.add(FileInspectionExceptionType.FILE_HASH_MISMATCH);
                }
            } catch (IOException e) {
                logger.warn("计算文件哈希失败，materialId: {}, file: {}, error: {}",
                        materialId, file.getAbsolutePath(), e.getMessage());
            }
        }

        if (PREVIEWABLE_EXTENSIONS.contains(extension.toLowerCase()) && actualSize > 0
                && actualSize <= MAX_PREVIEW_FILE_SIZE) {
            if (!isFileContentReadable(file)) {
                createOrUpdateRecord(material, FileInspectionExceptionType.PREVIEW_FAILED,
                        "文件内容无法正常读取，可能文件损坏", batchId);
                exceptions.add(FileInspectionExceptionType.PREVIEW_FAILED);
            }
        }

        resolveFixedIssues(material, exceptions, batchId);

        return exceptions;
    }

    private boolean isFileContentReadable(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            long totalRead = 0;
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                totalRead += bytesRead;
                if (totalRead > MAX_PREVIEW_FILE_SIZE) {
                    break;
                }
            }
            return totalRead == file.length() || totalRead > 0;
        } catch (IOException e) {
            logger.warn("文件读取校验失败: {}, error: {}", file.getAbsolutePath(), e.getMessage());
            return false;
        }
    }

    private FileInspectionRecord createOrUpdateRecord(Material material,
                                                      FileInspectionExceptionType exceptionType,
                                                      String detail,
                                                      String batchId) {
        FileInspectionRecord existing = inspectionRecordRepository
                .findFirstByMaterialIdAndExceptionTypeAndHandleStatusIn(
                        material.getId(), exceptionType.getCode(), UNRESOLVED_STATUSES)
                .orElse(null);

        if (existing != null) {
            existing.setExceptionDetail(detail);
            existing.setInspectionBatch(batchId);
            existing.setMaterialTitle(material.getTitle());
            existing.setFileUrl(material.getFileUrl());
            return inspectionRecordRepository.save(existing);
        }

        FileInspectionRecord record = new FileInspectionRecord();
        record.setMaterialId(material.getId());
        record.setMaterialTitle(material.getTitle());
        record.setFileUrl(material.getFileUrl());
        record.setExceptionType(exceptionType.getCode());
        record.setExceptionDetail(detail);
        record.setHandleStatus(FileInspectionHandleStatus.PENDING.getCode());
        record.setInspectionBatch(batchId);
        record.setExpectedFileSize(material.getFileSize());
        record.setExpectedFileHash(material.getFileHash());
        return inspectionRecordRepository.save(record);
    }

    private void resolveFixedIssues(Material material,
                                    List<FileInspectionExceptionType> currentExceptions,
                                    String batchId) {
        List<FileInspectionRecord> unresolved = inspectionRecordRepository
                .findByMaterialIdAndHandleStatusIn(material.getId(), UNRESOLVED_STATUSES);

        for (FileInspectionRecord record : unresolved) {
            FileInspectionExceptionType recordType = FileInspectionExceptionType.fromCode(record.getExceptionType());
            if (recordType != null && !currentExceptions.contains(recordType)) {
                record.setHandleStatus(FileInspectionHandleStatus.RESOLVED.getCode());
                record.setHandleRemark("自动恢复 - 巡检确认问题已解决，批次: " + batchId);
                record.setHandledAt(LocalDateTime.now());
                inspectionRecordRepository.save(record);
            }
        }
    }

    public Page<FileInspectionRecord> getRecordsPage(int page, int size,
                                                     Integer handleStatus,
                                                     Integer exceptionType,
                                                     String materialTitle,
                                                     LocalDateTime startTime,
                                                     LocalDateTime endTime) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "discoveredAt"));
        return inspectionRecordRepository.searchRecords(
                handleStatus, exceptionType, materialTitle, startTime, endTime, pageable);
    }

    public Page<FileInspectionRecord> getRecordsByMaterial(Long materialId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "discoveredAt"));
        return inspectionRecordRepository.findByMaterialId(materialId, pageable);
    }

    public FileInspectionRecord getRecordById(Long id) {
        return inspectionRecordRepository.findById(id).orElse(null);
    }

    @Transactional
    public boolean updateHandleStatus(Long id, Integer handleStatus, Long handlerId, String handleRemark) {
        FileInspectionHandleStatus status = FileInspectionHandleStatus.fromCode(handleStatus);
        if (status == null) {
            return false;
        }
        int updated = inspectionRecordRepository.updateHandleStatus(
                id, handleStatus, handlerId, handleRemark, LocalDateTime.now());
        return updated > 0;
    }

    @Transactional
    public int batchResolve(Long[] ids, Long handlerId, String remark) {
        int count = 0;
        for (Long id : ids) {
            if (updateHandleStatus(id, FileInspectionHandleStatus.RESOLVED.getCode(), handlerId, remark)) {
                count++;
            }
        }
        return count;
    }

    @Transactional
    public int batchIgnore(Long[] ids, Long handlerId, String remark) {
        int count = 0;
        for (Long id : ids) {
            if (updateHandleStatus(id, FileInspectionHandleStatus.IGNORED.getCode(), handlerId, remark)) {
                count++;
            }
        }
        return count;
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalUnresolved = inspectionRecordRepository.countByHandleStatusIn(UNRESOLVED_STATUSES);
        long affectedMaterials = inspectionRecordRepository.countDistinctMaterialsByHandleStatusIn(UNRESOLVED_STATUSES);
        stats.put("totalUnresolved", totalUnresolved);
        stats.put("affectedMaterials", affectedMaterials);

        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime weekStart = todayStart.minusDays(7);
        LocalDateTime monthStart = todayStart.minusDays(30);

        stats.put("todayDiscovered", inspectionRecordRepository.countByDiscoveredAtAfter(todayStart));
        stats.put("weekDiscovered", inspectionRecordRepository.countByDiscoveredAtAfter(weekStart));
        stats.put("monthDiscovered", inspectionRecordRepository.countByDiscoveredAtAfter(monthStart));

        List<Object[]> typeCounts = inspectionRecordRepository.countByExceptionType(UNRESOLVED_STATUSES);
        List<Map<String, Object>> typeStats = new ArrayList<>();
        for (Object[] row : typeCounts) {
            Integer typeCode = (Integer) row[0];
            Long count = (Long) row[1];
            FileInspectionExceptionType type = FileInspectionExceptionType.fromCode(typeCode);
            Map<String, Object> item = new HashMap<>();
            item.put("code", typeCode);
            item.put("name", type != null ? type.getName() : "未知");
            item.put("count", count);
            typeStats.add(item);
        }
        stats.put("byExceptionType", typeStats);

        List<Object[]> statusCounts = inspectionRecordRepository.countGroupByHandleStatus();
        List<Map<String, Object>> statusStats = new ArrayList<>();
        for (Object[] row : statusCounts) {
            Integer statusCode = (Integer) row[0];
            Long count = (Long) row[1];
            FileInspectionHandleStatus status = FileInspectionHandleStatus.fromCode(statusCode);
            Map<String, Object> item = new HashMap<>();
            item.put("code", statusCode);
            item.put("name", status != null ? status.getName() : "未知");
            item.put("count", count);
            statusStats.add(item);
        }
        stats.put("byHandleStatus", statusStats);

        return stats;
    }

    public List<Map<String, Object>> getExceptionTypeList() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (FileInspectionExceptionType type : FileInspectionExceptionType.values()) {
            Map<String, Object> item = new HashMap<>();
            item.put("code", type.getCode());
            item.put("name", type.getName());
            item.put("description", type.getDescription());
            list.add(item);
        }
        return list;
    }

    public List<Map<String, Object>> getHandleStatusList() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (FileInspectionHandleStatus status : FileInspectionHandleStatus.values()) {
            Map<String, Object> item = new HashMap<>();
            item.put("code", status.getCode());
            item.put("name", status.getName());
            list.add(item);
        }
        return list;
    }
}
