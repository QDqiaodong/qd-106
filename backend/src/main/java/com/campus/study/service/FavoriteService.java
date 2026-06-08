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

        List<Material> resultList = favoritePage.getContent().stream()
                .map(f -> materialMap.get(f.getMaterialId()))
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
}
