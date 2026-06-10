package com.campus.study.service;

import com.campus.study.entity.Material;
import com.campus.study.repository.MaterialRepository;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
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
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private ViewCountService viewCountService;

    public Page<Material> getMaterialPage(int page, int size, String keyword, Long categoryId, Long gradeId, Long subjectId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        if (keyword != null && !keyword.isEmpty()) {
            String cacheKey = SEARCH_HISTORY_KEY_PREFIX + keyword + ":" + page + ":" + size;
            @SuppressWarnings("unchecked")
            Page<Material> cachedPage = (Page<Material>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedPage != null) {
                enrichMaterialListViewCount(cachedPage.getContent());
                return cachedPage;
            }
            Page<Material> resultPage = materialRepository.findByTitleContainingAndStatus(keyword, 1, pageable);
            enrichMaterialListViewCount(resultPage.getContent());
            redisTemplate.opsForValue().set(cacheKey, resultPage, SEARCH_CACHE_TIMEOUT, TimeUnit.MINUTES);
            return resultPage;
        }
        
        boolean hasCategory = categoryId != null && categoryId > 0;
        boolean hasGrade = gradeId != null && gradeId > 0;
        boolean hasSubject = subjectId != null && subjectId > 0;
        
        Page<Material> resultPage;
        if (hasCategory || hasGrade || hasSubject) {
            if (hasCategory && hasGrade && hasSubject) {
                resultPage = materialRepository.findByCategoryIdAndGradeIdAndSubjectIdAndStatus(categoryId, gradeId, subjectId, 1, pageable);
            } else if (hasCategory) {
                resultPage = materialRepository.findByCategoryIdAndStatus(categoryId, 1, pageable);
            } else if (hasGrade) {
                resultPage = materialRepository.findByGradeIdAndStatus(gradeId, 1, pageable);
            } else {
                resultPage = materialRepository.findBySubjectIdAndStatus(subjectId, 1, pageable);
            }
        } else {
            resultPage = materialRepository.findByStatus(1, pageable);
        }
        enrichMaterialListViewCount(resultPage.getContent());
        return resultPage;
    }

    @SuppressWarnings("unchecked")
    public List<Material> getHotMaterials() {
        List<Material> cachedList = (List<Material>) redisTemplate.opsForValue().get(HOT_MATERIALS_CACHE_KEY);
        if (cachedList != null) {
            return cachedList;
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
            return cachedList;
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
            return cachedList;
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
            return cachedList;
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
}
