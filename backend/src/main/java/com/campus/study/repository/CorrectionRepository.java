package com.campus.study.repository;

import com.campus.study.entity.Correction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorrectionRepository extends JpaRepository<Correction, Long> {

    Page<Correction> findByMaterialId(Long materialId, Pageable pageable);

    List<Correction> findByMaterialId(Long materialId);

    Page<Correction> findByUserId(Long userId, Pageable pageable);

    Page<Correction> findByMaterialIdIn(List<Long> materialIds, Pageable pageable);

    Page<Correction> findByMaterialIdInAndStatus(List<Long> materialIds, Integer status, Pageable pageable);

    long countByMaterialIdInAndStatus(List<Long> materialIds, Integer status);
}
