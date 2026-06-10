package com.campus.study.repository;

import com.campus.study.entity.FilterSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FilterSnapshotRepository extends JpaRepository<FilterSnapshot, Long> {

    List<FilterSnapshot> findByUserIdOrderBySortAscCreatedAtDesc(Long userId);

    Optional<FilterSnapshot> findByIdAndUserId(Long id, Long userId);

    void deleteByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndName(Long userId, String name);
}
