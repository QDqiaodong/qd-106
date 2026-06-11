package com.campus.study.service;

import com.campus.study.entity.Material;
import com.campus.study.util.FileUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ChunkUploadService {

    private static final String CHUNK_UPLOAD_KEY_PREFIX = "chunk:upload:";
    private static final long UPLOAD_EXPIRE_HOURS = 24;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private FileUtil fileUtil;

    @Resource
    private MaterialService materialService;

    @Value("${file.upload.path:/app/uploads}")
    private String uploadPath;

    @Value("${file.chunk.path:/app/uploads/chunks}")
    private String chunkPath;

    public Map<String, Object> initUpload(String fileName, long fileSize, int chunkSize, String fileMd5) {
        String uploadId = UUID.randomUUID().toString().replace("-", "");
        int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);

        String extension = fileUtil.getFileExtension(fileName);
        if (!fileUtil.isValidExtension(extension)) {
            throw new IllegalArgumentException("不支持的文件格式");
        }

        Map<String, Object> uploadInfo = new HashMap<>();
        uploadInfo.put("uploadId", uploadId);
        uploadInfo.put("fileName", fileName);
        uploadInfo.put("fileSize", fileSize);
        uploadInfo.put("chunkSize", chunkSize);
        uploadInfo.put("totalChunks", totalChunks);
        uploadInfo.put("fileMd5", fileMd5 != null ? fileMd5 : "");
        uploadInfo.put("uploadedChunks", new ArrayList<Integer>());
        uploadInfo.put("status", "init");
        uploadInfo.put("createdAt", System.currentTimeMillis());

        String key = CHUNK_UPLOAD_KEY_PREFIX + uploadId;
        redisTemplate.opsForValue().set(key, uploadInfo, UPLOAD_EXPIRE_HOURS, TimeUnit.HOURS);

        String chunkDir = getChunkDir(uploadId);
        File dir = new File(chunkDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("uploadId", uploadId);
        result.put("totalChunks", totalChunks);
        result.put("chunkSize", chunkSize);
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadChunk(String uploadId, int chunkIndex, MultipartFile chunk) throws IOException {
        String key = CHUNK_UPLOAD_KEY_PREFIX + uploadId;
        Map<String, Object> uploadInfo = (Map<String, Object>) redisTemplate.opsForValue().get(key);

        if (uploadInfo == null) {
            throw new IllegalArgumentException("上传任务不存在或已过期");
        }

        String currentStatus = (String) uploadInfo.get("status");
        if ("cancelled".equals(currentStatus)) {
            throw new IllegalArgumentException("上传任务已取消");
        }

        int totalChunks = (int) uploadInfo.get("totalChunks");
        if (chunkIndex < 0 || chunkIndex >= totalChunks) {
            throw new IllegalArgumentException("分片索引无效");
        }

        String chunkDir = getChunkDir(uploadId);
        String chunkFileName = chunkIndex + ".part";
        File chunkFile = new File(chunkDir, chunkFileName);
        chunk.transferTo(chunkFile);

        List<Integer> uploadedChunks = (List<Integer>) uploadInfo.get("uploadedChunks");
        if (!uploadedChunks.contains(chunkIndex)) {
            uploadedChunks.add(chunkIndex);
            Collections.sort(uploadedChunks);
            uploadInfo.put("uploadedChunks", uploadedChunks);
        }

        uploadInfo.put("status", "uploading");
        redisTemplate.opsForValue().set(key, uploadInfo, UPLOAD_EXPIRE_HOURS, TimeUnit.HOURS);

        Map<String, Object> result = new HashMap<>();
        result.put("uploadId", uploadId);
        result.put("chunkIndex", chunkIndex);
        result.put("uploadedChunks", uploadedChunks.size());
        result.put("totalChunks", totalChunks);
        result.put("progress", (double) uploadedChunks.size() / totalChunks);
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getUploadStatus(String uploadId) {
        String key = CHUNK_UPLOAD_KEY_PREFIX + uploadId;
        Map<String, Object> uploadInfo = (Map<String, Object>) redisTemplate.opsForValue().get(key);

        if (uploadInfo == null) {
            return null;
        }

        int totalChunks = (int) uploadInfo.get("totalChunks");
        List<Integer> uploadedChunks = (List<Integer>) uploadInfo.get("uploadedChunks");

        List<Map<String, Object>> chunks = IntStream.range(0, totalChunks)
                .mapToObj(i -> {
                    Map<String, Object> chunkInfo = new HashMap<>();
                    chunkInfo.put("index", i);
                    chunkInfo.put("uploaded", uploadedChunks.contains(i));
                    chunkInfo.put("status", uploadedChunks.contains(i) ? "completed" : "waiting");
                    return chunkInfo;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("uploadId", uploadId);
        result.put("fileName", uploadInfo.get("fileName"));
        result.put("fileSize", uploadInfo.get("fileSize"));
        result.put("totalChunks", totalChunks);
        result.put("uploadedChunks", uploadedChunks.size());
        result.put("progress", (double) uploadedChunks.size() / totalChunks);
        result.put("status", uploadInfo.get("status"));
        result.put("chunks", chunks);
        return result;
    }

    @SuppressWarnings("unchecked")
    public Material mergeChunks(String uploadId, String title, String description,
                                 Long categoryId, Long gradeId, Long subjectId, Long userId) throws IOException {
        String key = CHUNK_UPLOAD_KEY_PREFIX + uploadId;
        Map<String, Object> uploadInfo = (Map<String, Object>) redisTemplate.opsForValue().get(key);

        if (uploadInfo == null) {
            throw new IllegalArgumentException("上传任务不存在或已过期");
        }

        String currentStatus = (String) uploadInfo.get("status");
        if ("cancelled".equals(currentStatus)) {
            throw new IllegalArgumentException("上传任务已取消");
        }
        if ("merging".equals(currentStatus)) {
            throw new IllegalArgumentException("上传任务正在合并中");
        }

        uploadInfo.put("status", "merging");
        redisTemplate.opsForValue().set(key, uploadInfo, UPLOAD_EXPIRE_HOURS, TimeUnit.HOURS);

        String fileName = (String) uploadInfo.get("fileName");
        long fileSize = (long) uploadInfo.get("fileSize");
        int totalChunks = (int) uploadInfo.get("totalChunks");
        List<Integer> uploadedChunks = (List<Integer>) uploadInfo.get("uploadedChunks");

        if (uploadedChunks.size() != totalChunks) {
            throw new IllegalArgumentException("分片未全部上传完成");
        }

        String extension = fileUtil.getFileExtension(fileName);
        String newFilename = UUID.randomUUID().toString() + "." + extension;
        String materialsDir = uploadPath + File.separator + "materials";
        File dir = new File(materialsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File mergedFile = new File(materialsDir, newFilename);
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(mergedFile))) {
            for (int i = 0; i < totalChunks; i++) {
                String chunkFileName = getChunkDir(uploadId) + File.separator + i + ".part";
                File chunkFile = new File(chunkFileName);
                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(chunkFile))) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                    }
                }
            }
            bos.flush();
        }

        deleteChunkDir(uploadId);

        Material material = new Material();
        material.setTitle(title);
        material.setDescription(description != null ? description : "");
        material.setCategoryId(categoryId != null ? categoryId : 0L);
        material.setGradeId(gradeId != null ? gradeId : 0L);
        material.setSubjectId(subjectId != null ? subjectId : 0L);
        material.setFileUrl("/uploads/materials/" + newFilename);
        material.setFileSize(fileSize);

        Material saved = materialService.uploadMaterial(material, userId);

        redisTemplate.delete(key);

        return saved;
    }

    @SuppressWarnings("unchecked")
    public boolean cancelUpload(String uploadId) {
        String key = CHUNK_UPLOAD_KEY_PREFIX + uploadId;
        Map<String, Object> uploadInfo = (Map<String, Object>) redisTemplate.opsForValue().get(key);
        if (uploadInfo == null) {
            return false;
        }

        String currentStatus = (String) uploadInfo.get("status");
        if ("merging".equals(currentStatus) || "completed".equals(currentStatus)) {
            return false;
        }

        redisTemplate.delete(key);

        deleteChunkDir(uploadId);
        return true;
    }

    private String getChunkDir(String uploadId) {
        return chunkPath + File.separator + uploadId;
    }

    private void deleteChunkDir(String uploadId) {
        String chunkDir = getChunkDir(uploadId);
        try {
            Path dirPath = Paths.get(chunkDir);
            if (Files.exists(dirPath)) {
                Files.walk(dirPath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            // ignore
        }
    }
}
