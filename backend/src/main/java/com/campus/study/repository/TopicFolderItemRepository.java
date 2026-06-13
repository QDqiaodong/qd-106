package com.campus.study.repository;

import com.campus.study.entity.TopicFolderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TopicFolderItemRepository extends JpaRepository<TopicFolderItem, Long> {

    List<TopicFolderItem> findByFolderIdOrderBySortAscIdAsc(Long folderId);

    int countByFolderId(Long folderId);

    @Modifying
    @Transactional
    void deleteByFolderId(Long folderId);
}
