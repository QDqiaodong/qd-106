package com.campus.study.repository;

import com.campus.study.entity.Material;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {

    Page<Material> findByStatus(Integer status, Pageable pageable);

    Page<Material> findByTitleContainingAndStatus(String title, Integer status, Pageable pageable);

    Page<Material> findByCategoryIdAndStatus(Long categoryId, Integer status, Pageable pageable);

    Page<Material> findByGradeIdAndStatus(Long gradeId, Integer status, Pageable pageable);

    Page<Material> findBySubjectIdAndStatus(Long subjectId, Integer status, Pageable pageable);

    Page<Material> findByCategoryIdAndGradeIdAndSubjectIdAndStatus(
            Long categoryId, Long gradeId, Long subjectId, Integer status, Pageable pageable);

    Page<Material> findByTitleContainingAndCategoryIdAndStatus(String title, Long categoryId, Integer status, Pageable pageable);

    Page<Material> findByTitleContainingAndGradeIdAndStatus(String title, Long gradeId, Integer status, Pageable pageable);

    Page<Material> findByTitleContainingAndSubjectIdAndStatus(String title, Long subjectId, Integer status, Pageable pageable);

    Page<Material> findByTitleContainingAndCategoryIdAndGradeIdAndSubjectIdAndStatus(
            String title, Long categoryId, Long gradeId, Long subjectId, Integer status, Pageable pageable);

    Page<Material> findByTitleContainingAndCategoryIdAndGradeIdAndStatus(
            String title, Long categoryId, Long gradeId, Integer status, Pageable pageable);

    Page<Material> findByTitleContainingAndCategoryIdAndSubjectIdAndStatus(
            String title, Long categoryId, Long subjectId, Integer status, Pageable pageable);

    Page<Material> findByTitleContainingAndGradeIdAndSubjectIdAndStatus(
            String title, Long gradeId, Long subjectId, Integer status, Pageable pageable);

    Page<Material> findByUserId(Long userId, Pageable pageable);

    List<Material> findByUserId(Long userId);

    List<Material> findTop10ByStatusOrderByViewCountDesc(Integer status);

    List<Material> findByStatus(Integer status);

    @Modifying
    @Query("UPDATE Material m SET m.viewCount = m.viewCount + 1 WHERE m.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Material m SET m.viewCount = m.viewCount + :increment WHERE m.id = :id")
    int updateViewCount(@Param("id") Long id, @Param("increment") int increment);

    List<Material> findByFileHashAndStatus(String fileHash, Integer status);

    List<Material> findByFileSizeBetweenAndStatus(Long minSize, Long maxSize, Integer status);

    List<Material> findByTitleContainingAndStatus(String title, Integer status);

    @Query("SELECT m FROM Material m WHERE m.status = 1 AND m.fileHash IS NOT NULL AND m.fileHash <> ''")
    List<Material> findAllWithFileHash();
}
