package com.campus.study.repository;

import com.campus.study.entity.KeywordFrequency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface KeywordFrequencyRepository extends JpaRepository<KeywordFrequency, Long> {

    Optional<KeywordFrequency> findByWordAndSubjectId(String word, Long subjectId);

    List<KeywordFrequency> findBySubjectIdOrderByFrequencyDesc(Long subjectId);

    List<KeywordFrequency> findBySubjectIdAndLastSeenAtAfterOrderByFrequencyDesc(Long subjectId, LocalDateTime since);

    @Query("SELECT kf FROM KeywordFrequency kf WHERE kf.subjectId = 0 AND kf.lastSeenAt > :since ORDER BY kf.frequency DESC")
    List<KeywordFrequency> findGlobalRecent(@Param("since") LocalDateTime since);

    @Query("SELECT kf FROM KeywordFrequency kf ORDER BY kf.frequency DESC")
    List<KeywordFrequency> findAllOrderByFrequencyDesc();

    @Modifying
    @Query("DELETE FROM KeywordFrequency kf WHERE kf.subjectId = :subjectId")
    void deleteBySubjectId(@Param("subjectId") Long subjectId);

    @Modifying
    @Query("DELETE FROM KeywordFrequency kf")
    void deleteAllKeywords();

    boolean existsByWordAndSubjectId(String word, Long subjectId);
}
