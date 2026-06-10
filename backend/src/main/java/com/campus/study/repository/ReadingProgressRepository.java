package com.campus.study.repository;

import com.campus.study.entity.ReadingProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, Long> {

    Optional<ReadingProgress> findByUserIdAndMaterialId(Long userId, Long materialId);

    void deleteByUserIdAndMaterialId(Long userId, Long materialId);
}
