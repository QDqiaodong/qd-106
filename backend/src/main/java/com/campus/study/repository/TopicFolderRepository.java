package com.campus.study.repository;

import com.campus.study.entity.TopicFolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TopicFolderRepository extends JpaRepository<TopicFolder, Long> {

    List<TopicFolder> findByStatusOrderBySortAscCreatedAtDesc(Integer status);

    Page<TopicFolder> findByStatusOrderBySortAscCreatedAtDesc(Integer status, Pageable pageable);

    Page<TopicFolder> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Integer status, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE TopicFolder f SET f.viewCount = f.viewCount + 1 WHERE f.id = :id")
    int incrementViewCount(Long id);
}
