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

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MaterialService {

    private static final String HOT_MATERIALS_CACHE_KEY = "material:hot";
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

    public boolean recordView(Long materialId, Long userId, String ip, String userAgent) {
        boolean isNewView = viewCountService.recordView(materialId, userId, ip, userAgent);
        if (isNewView) {
            redisTemplate.delete(HOT_MATERIALS_CACHE_KEY);
        }
        return isNewView;
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
        redisTemplate.delete(HOT_MATERIALS_CACHE_KEY);
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
            redisTemplate.delete(HOT_MATERIALS_CACHE_KEY);
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
