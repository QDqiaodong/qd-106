package com.campus.study.repository;

import com.campus.study.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndMaterialId(Long userId, Long materialId);

    Page<Favorite> findByUserId(Long userId, Pageable pageable);

    boolean existsByUserIdAndMaterialId(Long userId, Long materialId);

    void deleteByUserIdAndMaterialId(Long userId, Long materialId);
}
