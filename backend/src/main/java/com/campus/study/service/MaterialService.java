package com.campus.study.service;

import com.campus.study.entity.Category;
import com.campus.study.entity.Grade;
import com.campus.study.entity.Material;
import com.campus.study.entity.Subject;
import com.campus.study.enums.PreviewErrorCode;
import com.campus.study.repository.CategoryRepository;
import com.campus.study.repository.GradeRepository;
import com.campus.study.repository.MaterialRepository;
import com.campus.study.repository.SubjectRepository;
import com.campus.study.util.FileUtil;
import com.campus.study.vo.MaterialThumbnailVO;
import com.campus.study.vo.PreviewStatusVO;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MaterialService {

    private static final String HOT_MATERIALS_CACHE_KEY = "material:hot";
    private static final String HOT_MATERIALS_7D_CACHE_KEY = "material:hot:7d";
    private static final String HOT_MATERIALS_30D_CACHE_KEY = "material:hot:30d";
    private static final String HOT_MATERIALS_SEMESTER_CACHE_KEY = "material:hot:semester";
    private static final String SEARCH_HISTORY_KEY_PREFIX = "material:search:";
    private static final long HOT_CACHE_TIMEOUT = 10;
    private static final long SEARCH_CACHE_TIMEOUT = 5;
    private static final Long DEFAULT_USER_ID = 1L;

    @Resource
    private MaterialRepository materialRepository;

    @Resource
    private GradeRepository gradeRepository;

    @Resource
    private SubjectRepository subjectRepository;

    @Resource
    private CategoryRepository categoryRepository;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private ViewCountService viewCountService;

    @Resource
    private FileUtil fileUtil;

    @Value("${file.upload.path:/app/uploads}")
    private String uploadPath;

    private static final List<String> PREVIEWABLE_EXTENSIONS = Arrays.asList(
            "pdf", "txt", "jpg", "jpeg", "png", "gif"
    );

    private static final long MAX_PREVIEW_FILE_SIZE = 50 * 1024 * 1024L;

    public PreviewStatusVO checkPreviewStatus(Long id) {
        PreviewStatusVO vo = new PreviewStatusVO();

        Material material = materialRepository.findById(id).orElse(null);
        if (material == null) {
            vo.setCode(PreviewErrorCode.MATERIAL_NOT_FOUND.getCode());
            vo.setMessage(PreviewErrorCode.MATERIAL_NOT_FOUND.getMessage());
            vo.setPreviewable(false);
            vo.setDownloadable(false);
            return vo;
        }

        if (material.getStatus() == null || material.getStatus() != 1) {
            vo.setCode(PreviewErrorCode.MATERIAL_OFFLINE.getCode());
            vo.setMessage(PreviewErrorCode.MATERIAL_OFFLINE.getMessage());
            vo.setPreviewable(false);
            vo.setDownloadable(false);
            return vo;
        }

        String fileUrl = material.getFileUrl();
        if (fileUrl == null || fileUrl.isEmpty()) {
            vo.setCode(PreviewErrorCode.FILE_NOT_FOUND.getCode());
            vo.setMessage(PreviewErrorCode.FILE_NOT_FOUND.getMessage());
            vo.setPreviewable(false);
            vo.setDownloadable(false);
            return vo;
        }

        String relativePath = fileUrl.startsWith("/uploads/") ? fileUrl.substring(9) : fileUrl;
        File file = new File(uploadPath, relativePath);

        if (!file.exists()) {
            vo.setCode(PreviewErrorCode.FILE_NOT_FOUND.getCode());
            vo.setMessage(PreviewErrorCode.FILE_NOT_FOUND.getMessage());
            vo.setPreviewable(false);
            vo.setDownloadable(false);
            vo.setFallbackTip("文件已丢失，请联系管理员");
            return vo;
        }

        if (file.length() == 0) {
            vo.setCode(PreviewErrorCode.FILE_EMPTY.getCode());
            vo.setMessage(PreviewErrorCode.FILE_EMPTY.getMessage());
            vo.setPreviewable(false);
            vo.setDownloadable(true);
            vo.setFileSize(0L);
            return vo;
        }

        String extension = fileUtil.getFileExtension(file.getName());
        vo.setFileType(extension);
        vo.setFileSize(file.length());
        vo.setDownloadable(true);

        if (!fileUtil.isValidExtension(extension)) {
            vo.setCode(PreviewErrorCode.FORMAT_NOT_SUPPORTED.getCode());
            vo.setMessage(PreviewErrorCode.FORMAT_NOT_SUPPORTED.getMessage());
            vo.setPreviewable(false);
            vo.setFallbackTip("该文件格式不被系统支持");
            return vo;
        }

        if (!PREVIEWABLE_EXTENSIONS.contains(extension.toLowerCase())) {
            vo.setCode(PreviewErrorCode.FORMAT_NOT_SUPPORTED.getCode());
            vo.setMessage(PreviewErrorCode.FORMAT_NOT_SUPPORTED.getMessage());
            vo.setPreviewable(false);
            vo.setFallbackTip("该格式暂不支持在线预览，建议下载后查看");
            return vo;
        }

        if (file.length() > MAX_PREVIEW_FILE_SIZE) {
            vo.setCode(PreviewErrorCode.FILE_TOO_LARGE.getCode());
            vo.setMessage(PreviewErrorCode.FILE_TOO_LARGE.getMessage());
            vo.setPreviewable(false);
            vo.setFallbackTip("文件过大，建议下载后查看");
            return vo;
        }

        try {
            vo.setCode(PreviewErrorCode.SUCCESS.getCode());
            vo.setMessage(PreviewErrorCode.SUCCESS.getMessage());
            vo.setPreviewable(true);
            vo.setPreviewUrl("/materials/" + id + "/preview");
        } catch (Exception e) {
            vo.setCode(PreviewErrorCode.PREVIEW_GENERATE_ERROR.getCode());
            vo.setMessage(PreviewErrorCode.PREVIEW_GENERATE_ERROR.getMessage());
            vo.setPreviewable(false);
            vo.setFallbackTip("预览生成失败，请稍后重试或下载查看");
        }

        return vo;
    }

    public Page<Material> getMaterialPage(int page, int size, String keyword, Long categoryId, Long gradeId, Long subjectId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        boolean hasKeyword = keyword != null && !keyword.isEmpty();
        boolean hasCategory = categoryId != null && categoryId > 0;
        boolean hasGrade = gradeId != null && gradeId > 0;
        boolean hasSubject = subjectId != null && subjectId > 0;

        String cacheKey = SEARCH_HISTORY_KEY_PREFIX + (hasKeyword ? keyword : "") + ":c" + (hasCategory ? categoryId : "0") + ":g" + (hasGrade ? gradeId : "0") + ":s" + (hasSubject ? subjectId : "0") + ":" + page + ":" + size;
        @SuppressWarnings("unchecked")
        Page<Material> cachedPage = (Page<Material>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedPage != null) {
            enrichMaterialListViewCount(cachedPage.getContent());
            return cachedPage;
        }

        Page<Material> resultPage;
        if (hasKeyword && hasCategory && hasGrade && hasSubject) {
            resultPage = materialRepository.findByTitleContainingAndCategoryIdAndGradeIdAndSubjectIdAndStatus(keyword, categoryId, gradeId, subjectId, 1, pageable);
        } else if (hasKeyword && hasCategory && hasGrade) {
            resultPage = materialRepository.findByTitleContainingAndCategoryIdAndGradeIdAndStatus(keyword, categoryId, gradeId, 1, pageable);
        } else if (hasKeyword && hasCategory && hasSubject) {
            resultPage = materialRepository.findByTitleContainingAndCategoryIdAndSubjectIdAndStatus(keyword, categoryId, subjectId, 1, pageable);
        } else if (hasKeyword && hasGrade && hasSubject) {
            resultPage = materialRepository.findByTitleContainingAndGradeIdAndSubjectIdAndStatus(keyword, gradeId, subjectId, 1, pageable);
        } else if (hasKeyword && hasCategory) {
            resultPage = materialRepository.findByTitleContainingAndCategoryIdAndStatus(keyword, categoryId, 1, pageable);
        } else if (hasKeyword && hasGrade) {
            resultPage = materialRepository.findByTitleContainingAndGradeIdAndStatus(keyword, gradeId, 1, pageable);
        } else if (hasKeyword && hasSubject) {
            resultPage = materialRepository.findByTitleContainingAndSubjectIdAndStatus(keyword, subjectId, 1, pageable);
        } else if (hasKeyword) {
            resultPage = materialRepository.findByTitleContainingAndStatus(keyword, 1, pageable);
        } else if (hasCategory && hasGrade && hasSubject) {
            resultPage = materialRepository.findByCategoryIdAndGradeIdAndSubjectIdAndStatus(categoryId, gradeId, subjectId, 1, pageable);
        } else if (hasCategory) {
            resultPage = materialRepository.findByCategoryIdAndStatus(categoryId, 1, pageable);
        } else if (hasGrade) {
            resultPage = materialRepository.findByGradeIdAndStatus(gradeId, 1, pageable);
        } else if (hasSubject) {
            resultPage = materialRepository.findBySubjectIdAndStatus(subjectId, 1, pageable);
        } else {
            resultPage = materialRepository.findByStatus(1, pageable);
        }

        enrichMaterialListViewCount(resultPage.getContent());
        redisTemplate.opsForValue().set(cacheKey, resultPage, SEARCH_CACHE_TIMEOUT, TimeUnit.MINUTES);
        return resultPage;
    }

    @SuppressWarnings("unchecked")
    public List<Material> getHotMaterials() {
        List<Material> cachedList = (List<Material>) redisTemplate.opsForValue().get(HOT_MATERIALS_CACHE_KEY);
        if (cachedList != null) {
            List<Material> filtered = cachedList.stream()
                    .filter(m -> m.getStatus() != null && m.getStatus() == 1)
                    .collect(Collectors.toList());
            if (filtered.size() == cachedList.size()) {
                return filtered;
            }
        }
        List<Material> hotList = materialRepository.findTop10ByStatusOrderByViewCountDesc(1);
        for (Material material : hotList) {
            int redisViewCount = viewCountService.getViewCount(material.getId());
            material.setViewCount(material.getViewCount() + redisViewCount);
        }
        hotList.sort((m1, m2) -> Integer.compare(m2.getViewCount(), m1.getViewCount()));
        if (hotList.size() > 10) {
            hotList = hotList.subList(0, 10);
        }
        redisTemplate.opsForValue().set(HOT_MATERIALS_CACHE_KEY, hotList, HOT_CACHE_TIMEOUT, TimeUnit.MINUTES);
        return hotList;
    }

    public Map<String, Object> getAllHotMaterials() {
        Map<String, Object> result = new HashMap<>();
        result.put("hot7d", getHotMaterials7Days());
        result.put("hot30d", getHotMaterials30Days());
        result.put("hotSemester", getHotMaterialsSemester());
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Material> getHotMaterials7Days() {
        List<Material> cachedList = (List<Material>) redisTemplate.opsForValue().get(HOT_MATERIALS_7D_CACHE_KEY);
        if (cachedList != null) {
            List<Material> filtered = cachedList.stream()
                    .filter(m -> m.getStatus() != null && m.getStatus() == 1)
                    .collect(Collectors.toList());
            if (filtered.size() == cachedList.size()) {
                return filtered;
            }
        }
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);
        List<Material> hotList = getHotMaterialsInRange(startDate, today, 10);
        redisTemplate.opsForValue().set(HOT_MATERIALS_7D_CACHE_KEY, hotList, HOT_CACHE_TIMEOUT, TimeUnit.MINUTES);
        return hotList;
    }

    @SuppressWarnings("unchecked")
    public List<Material> getHotMaterials30Days() {
        List<Material> cachedList = (List<Material>) redisTemplate.opsForValue().get(HOT_MATERIALS_30D_CACHE_KEY);
        if (cachedList != null) {
            List<Material> filtered = cachedList.stream()
                    .filter(m -> m.getStatus() != null && m.getStatus() == 1)
                    .collect(Collectors.toList());
            if (filtered.size() == cachedList.size()) {
                return filtered;
            }
        }
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(29);
        List<Material> hotList = getHotMaterialsInRange(startDate, today, 10);
        redisTemplate.opsForValue().set(HOT_MATERIALS_30D_CACHE_KEY, hotList, HOT_CACHE_TIMEOUT, TimeUnit.MINUTES);
        return hotList;
    }

    @SuppressWarnings("unchecked")
    public List<Material> getHotMaterialsSemester() {
        List<Material> cachedList = (List<Material>) redisTemplate.opsForValue().get(HOT_MATERIALS_SEMESTER_CACHE_KEY);
        if (cachedList != null) {
            List<Material> filtered = cachedList.stream()
                    .filter(m -> m.getStatus() != null && m.getStatus() == 1)
                    .collect(Collectors.toList());
            if (filtered.size() == cachedList.size()) {
                return filtered;
            }
        }
        LocalDate[] semesterRange = getSemesterDateRange();
        List<Material> hotList = getHotMaterialsInRange(semesterRange[0], semesterRange[1], 10);
        redisTemplate.opsForValue().set(HOT_MATERIALS_SEMESTER_CACHE_KEY, hotList, HOT_CACHE_TIMEOUT, TimeUnit.MINUTES);
        return hotList;
    }

    private List<Material> getHotMaterialsInRange(LocalDate startDate, LocalDate endDate, int limit) {
        Map<Long, Integer> rangeViewCounts = viewCountService.getViewCountsInDateRange(startDate, endDate);

        if (rangeViewCounts.isEmpty()) {
            return new ArrayList<>();
        }

        List<Material> allMaterials = materialRepository.findByStatus(1);
        Map<Long, Material> materialMap = allMaterials.stream()
                .collect(Collectors.toMap(Material::getId, m -> m));

        List<Material> resultList = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : rangeViewCounts.entrySet()) {
            Material mat = materialMap.get(entry.getKey());
            if (mat != null && entry.getValue() != null && entry.getValue() > 0) {
                Material copy = new Material();
                copy.setId(mat.getId());
                copy.setTitle(mat.getTitle());
                copy.setDescription(mat.getDescription());
                copy.setCover(mat.getCover());
                copy.setCategoryId(mat.getCategoryId());
                copy.setGradeId(mat.getGradeId());
                copy.setSubjectId(mat.getSubjectId());
                copy.setFileUrl(mat.getFileUrl());
                copy.setFileSize(mat.getFileSize());
                copy.setTotalPages(mat.getTotalPages());
                copy.setDownloadCount(mat.getDownloadCount());
                copy.setViewCount(entry.getValue());
                copy.setUserId(mat.getUserId());
                copy.setStatus(mat.getStatus());
                copy.setCreatedAt(mat.getCreatedAt());
                copy.setUpdatedAt(mat.getUpdatedAt());
                resultList.add(copy);
            }
        }

        resultList.sort(Comparator.comparingInt(Material::getViewCount).reversed()
                .thenComparing(Comparator.comparing(Material::getCreatedAt).reversed()));

        if (resultList.size() > limit) {
            resultList = resultList.subList(0, limit);
        }

        return resultList;
    }

    private LocalDate[] getSemesterDateRange() {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        Month month = today.getMonth();

        LocalDate startDate;
        LocalDate endDate;

        if (month.getValue() >= 2 && month.getValue() <= 7) {
            startDate = LocalDate.of(year, Month.FEBRUARY, 1);
            endDate = LocalDate.of(year, Month.JULY, 31);
        } else if (month.getValue() >= 8) {
            startDate = LocalDate.of(year, Month.AUGUST, 1);
            endDate = LocalDate.of(year + 1, Month.JANUARY, 31);
        } else {
            startDate = LocalDate.of(year - 1, Month.AUGUST, 1);
            endDate = LocalDate.of(year, Month.JANUARY, 31);
        }

        return new LocalDate[]{startDate, endDate};
    }

    public boolean recordView(Long materialId, Long userId, String ip, String userAgent) {
        boolean isNewView = viewCountService.recordView(materialId, userId, ip, userAgent);
        if (isNewView) {
            clearAllHotCaches();
        }
        return isNewView;
    }

    private void clearAllHotCaches() {
        redisTemplate.delete(HOT_MATERIALS_CACHE_KEY);
        redisTemplate.delete(HOT_MATERIALS_7D_CACHE_KEY);
        redisTemplate.delete(HOT_MATERIALS_30D_CACHE_KEY);
        redisTemplate.delete(HOT_MATERIALS_SEMESTER_CACHE_KEY);
    }

    public Material getMaterialByIdWithViewCount(Long id) {
        Material material = materialRepository.findById(id).orElse(null);
        if (material != null) {
            int redisViewCount = viewCountService.getViewCount(id);
            material.setViewCount(material.getViewCount() + redisViewCount);
        }
        return material;
    }

    public Material uploadMaterial(Material material, Long userId) {
        material.setUserId(userId);
        material.setStatus(1);
        material.setViewCount(0);
        material.setDownloadCount(0);
        Material saved = materialRepository.save(material);
        clearAllHotCaches();
        clearSearchCache();
        return saved;
    }

    @Transactional
    public boolean deleteMaterial(Long id, Long userId) {
        return materialRepository.findById(id).map(material -> {
            if (!material.getUserId().equals(userId)) {
                return false;
            }
            material.setStatus(0);
            materialRepository.save(material);
            clearAllHotCaches();
            clearSearchCache();
            return true;
        }).orElse(false);
    }

    public Page<Material> getMyUploads(int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Material> resultPage = materialRepository.findByUserId(userId, pageable);
        enrichMaterialListViewCount(resultPage.getContent());
        return resultPage;
    }

    private void enrichMaterialListViewCount(List<Material> materials) {
        if (materials == null || materials.isEmpty()) {
            return;
        }
        for (Material material : materials) {
            int redisViewCount = viewCountService.getViewCount(material.getId());
            material.setViewCount(material.getViewCount() + redisViewCount);
        }
    }

    public Material getMaterialById(Long id) {
        return materialRepository.findById(id).orElse(null);
    }

    private void clearSearchCache() {
        var keys = redisTemplate.keys(SEARCH_HISTORY_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public MaterialThumbnailVO getMaterialThumbnails(Long id, int limit) {
        Material material = materialRepository.findById(id).orElse(null);
        if (material == null) {
            return null;
        }
        if (material.getStatus() == null || material.getStatus() != 1) {
            return null;
        }

        MaterialThumbnailVO vo = new MaterialThumbnailVO();
        vo.setId(material.getId());
        vo.setTitle(material.getTitle());
        vo.setDescription(material.getDescription());
        vo.setCategoryId(material.getCategoryId());
        vo.setGradeId(material.getGradeId());
        vo.setSubjectId(material.getSubjectId());
        vo.setTotalPages(material.getTotalPages());

        if (material.getCategoryId() != null && material.getCategoryId() > 0) {
            Category category = categoryRepository.findById(material.getCategoryId()).orElse(null);
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }
        if (material.getGradeId() != null && material.getGradeId() > 0) {
            Grade grade = gradeRepository.findById(material.getGradeId()).orElse(null);
            if (grade != null) {
                vo.setGradeName(grade.getName());
            }
        }
        if (material.getSubjectId() != null && material.getSubjectId() > 0) {
            Subject subject = subjectRepository.findById(material.getSubjectId()).orElse(null);
            if (subject != null) {
                vo.setSubjectName(subject.getName());
            }
        }

        List<MaterialThumbnailVO.ThumbnailItem> thumbnails = new ArrayList<>();
        String cover = material.getCover();
        int totalPages = material.getTotalPages() != null ? material.getTotalPages() : 0;
        int pageCount = Math.min(limit, Math.max(totalPages, 1));

        for (int i = 1; i <= pageCount; i++) {
            MaterialThumbnailVO.ThumbnailItem item = new MaterialThumbnailVO.ThumbnailItem();
            item.setPageNumber(i);
            if (i == 1 && cover != null && !cover.isEmpty()) {
                item.setImageUrl(cover);
            } else {
                item.setImageUrl(generatePlaceholderThumbnail(material.getId(), i));
            }
            item.setLabel(i == 1 ? "封面" : "第" + i + "页");
            thumbnails.add(item);
        }

        vo.setThumbnails(thumbnails);
        return vo;
    }

    private String generatePlaceholderThumbnail(Long materialId, int pageNumber) {
        String[] colors = {
            "3B82F6", "10B981", "F59E0B", "EF4444", "8B5CF6",
            "EC4899", "06B6D4", "84CC16", "F97316", "6366F1"
        };
        String color = colors[(int) ((materialId + pageNumber) % colors.length)];
        String text = "P" + pageNumber;
        return "data:image/svg+xml;charset=UTF-8," +
            "%3Csvg xmlns='http://www.w3.org/2000/svg' width='200' height='280'%3E" +
            "%3Crect width='200' height='280' fill='%23" + color + "'/%3E" +
            "%3Crect x='10' y='10' width='180' height='260' fill='white' fill-opacity='0.9' rx='4'/%3E" +
            "%3Ctext x='100' y='145' font-family='Arial,sans-serif' font-size='48' " +
            "font-weight='bold' fill='%23" + color + "' text-anchor='middle' " +
            "dominant-baseline='middle'%3E" + text + "%3C/text%3E" +
            "%3Ctext x='100' y='200' font-family='Arial,sans-serif' font-size='12' " +
            "fill='%23999' text-anchor='middle'%3E第 " + pageNumber + " 页%3C/text%3E" +
            "%3C/svg%3E";
    }

    @Transactional
    public int recordDownload(Long id) {
        Material material = materialRepository.findById(id).orElse(null);
        if (material == null) {
            return -1;
        }
        material.setDownloadCount(material.getDownloadCount() + 1);
        materialRepository.save(material);
        clearAllHotCaches();
        clearSearchCache();
        return material.getDownloadCount();
    }
}
