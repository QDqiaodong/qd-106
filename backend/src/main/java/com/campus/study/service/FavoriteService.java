package com.campus.study.service;

import com.campus.study.entity.Favorite;
import com.campus.study.entity.Material;
import com.campus.study.repository.FavoriteRepository;
import com.campus.study.repository.MaterialRepository;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    @Resource
    private FavoriteRepository favoriteRepository;

    @Resource
    private MaterialRepository materialRepository;

    public Map<String, Object> getMyFavorites(int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Favorite> favoritePage = favoriteRepository.findByUserId(userId, pageable);

        List<Long> materialIds = favoritePage.getContent().stream()
                .map(Favorite::getMaterialId)
                .collect(Collectors.toList());

        List<Material> materials = materialRepository.findAllById(materialIds);
        Map<Long, Material> materialMap = materials.stream()
                .collect(Collectors.toMap(Material::getId, m -> m));

        Map<Long, Favorite> favoriteMap = favoritePage.getContent().stream()
                .collect(Collectors.toMap(Favorite::getMaterialId, f -> f));

        List<Material> resultList = favoritePage.getContent().stream()
                .map(f -> {
                    Material m = materialMap.get(f.getMaterialId());
                    if (m != null) {
                        m.setReviewStatus(f.getReviewStatus() != null ? f.getReviewStatus() : 0);
                        if (f.getReviewedAt() != null) {
                            m.setReviewedAt(f.getReviewedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        }
                    }
                    return m;
                })
                .filter(m -> m != null)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", resultList);
        result.put("total", favoritePage.getTotalElements());
        result.put("page", page);
        result.put("size", size);

        return result;
    }

    public boolean addFavorite(Long userId, Long materialId) {
        if (favoriteRepository.existsByUserIdAndMaterialId(userId, materialId)) {
            return false;
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setMaterialId(materialId);
        favorite.setReviewStatus(0);
        favoriteRepository.save(favorite);
        return true;
    }

    @Transactional
    public boolean removeFavorite(Long userId, Long materialId) {
        if (!favoriteRepository.existsByUserIdAndMaterialId(userId, materialId)) {
            return false;
        }
        favoriteRepository.deleteByUserIdAndMaterialId(userId, materialId);
        return true;
    }

    public boolean isFavorite(Long userId, Long materialId) {
        return favoriteRepository.existsByUserIdAndMaterialId(userId, materialId);
    }

    @Transactional
    public boolean updateReviewStatus(Long userId, Long materialId, Integer status) {
        Favorite favorite = favoriteRepository.findByUserIdAndMaterialId(userId, materialId).orElse(null);
        if (favorite == null) {
            return false;
        }
        favorite.setReviewStatus(status);
        if (status != null && status > 0) {
            favorite.setReviewedAt(LocalDateTime.now());
        } else {
            favorite.setReviewedAt(null);
        }
        favoriteRepository.save(favorite);
        return true;
    }

    public Integer getReviewStatus(Long userId, Long materialId) {
        Favorite favorite = favoriteRepository.findByUserIdAndMaterialId(userId, materialId).orElse(null);
        return favorite != null && favorite.getReviewStatus() != null ? favorite.getReviewStatus() : 0;
    }
}
