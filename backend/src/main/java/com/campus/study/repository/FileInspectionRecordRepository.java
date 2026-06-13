package com.campus.study.repository;

import com.campus.study.entity.FileInspectionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileInspectionRecordRepository extends JpaRepository<FileInspectionRecord, Long> {

    Page<FileInspectionRecord> findByHandleStatus(Integer handleStatus, Pageable pageable);

    Page<FileInspectionRecord> findByExceptionType(Integer exceptionType, Pageable pageable);

    Page<FileInspectionRecord> findByMaterialId(Long materialId, Pageable pageable);

    List<FileInspectionRecord> findByMaterialIdAndHandleStatusIn(Long materialId, List<Integer> handleStatuses);

    Optional<FileInspectionRecord> findFirstByMaterialIdAndExceptionTypeAndHandleStatusIn(
            Long materialId, Integer exceptionType, List<Integer> handleStatuses);

    @Query("SELECT r.exceptionType, COUNT(r) FROM FileInspectionRecord r " +
           "WHERE r.handleStatus IN :statuses GROUP BY r.exceptionType")
    List<Object[]> countByExceptionType(@Param("statuses") List<Integer> statuses);

    @Query("SELECT COUNT(r) FROM FileInspectionRecord r WHERE r.handleStatus IN :statuses")
    long countByHandleStatusIn(@Param("statuses") List<Integer> statuses);

    @Query("SELECT COUNT(r) FROM FileInspectionRecord r WHERE r.discoveredAt >= :startTime")
    long countByDiscoveredAtAfter(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT r.handleStatus, COUNT(r) FROM FileInspectionRecord r GROUP BY r.handleStatus")
    List<Object[]> countGroupByHandleStatus();

    @Query("SELECT COUNT(DISTINCT r.materialId) FROM FileInspectionRecord r WHERE r.handleStatus IN :statuses")
    long countDistinctMaterialsByHandleStatusIn(@Param("statuses") List<Integer> statuses);

    @Query("SELECT r FROM FileInspectionRecord r WHERE r.inspectionBatch = :batch")
    List<FileInspectionRecord> findByInspectionBatch(@Param("batch") String batch);

    @Modifying
    @Query("UPDATE FileInspectionRecord r SET r.handleStatus = :handleStatus, r.handlerId = :handlerId, " +
           "r.handleRemark = :handleRemark, r.handledAt = :handledAt WHERE r.id = :id")
    int updateHandleStatus(@Param("id") Long id,
                           @Param("handleStatus") Integer handleStatus,
                           @Param("handlerId") Long handlerId,
                           @Param("handleRemark") String handleRemark,
                           @Param("handledAt") LocalDateTime handledAt);

    @Modifying
    @Query("UPDATE FileInspectionRecord r SET r.handleStatus = :newStatus WHERE r.materialId = :materialId " +
           "AND r.exceptionType = :exceptionType AND r.handleStatus IN :statuses")
    int updateStatusByMaterialAndExceptionType(@Param("materialId") Long materialId,
                                               @Param("exceptionType") Integer exceptionType,
                                               @Param("newStatus") Integer newStatus,
                                               @Param("statuses") List<Integer> statuses);

    @Query("SELECT r FROM FileInspectionRecord r WHERE " +
           "(:handleStatus IS NULL OR r.handleStatus = :handleStatus) AND " +
           "(:exceptionType IS NULL OR r.exceptionType = :exceptionType) AND " +
           "(:materialTitle IS NULL OR r.materialTitle LIKE %:materialTitle%) AND " +
           "(:startTime IS NULL OR r.discoveredAt >= :startTime) AND " +
           "(:endTime IS NULL OR r.discoveredAt <= :endTime)")
    Page<FileInspectionRecord> searchRecords(
            @Param("handleStatus") Integer handleStatus,
            @Param("exceptionType") Integer exceptionType,
            @Param("materialTitle") String materialTitle,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);
}
