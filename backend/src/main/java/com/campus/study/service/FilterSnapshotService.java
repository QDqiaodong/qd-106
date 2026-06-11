package com.campus.study.service;

import com.campus.study.entity.FilterSnapshot;
import com.campus.study.repository.FilterSnapshotRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FilterSnapshotService {

    @Resource
    private FilterSnapshotRepository filterSnapshotRepository;

    @Resource
    private CategoryValidationService categoryValidationService;

    public List<FilterSnapshot> getMySnapshots(Long userId) {
        return filterSnapshotRepository.findByUserIdOrderBySortAscCreatedAtDesc(userId);
    }

    public FilterSnapshot createSnapshot(Long userId, String name, String keyword,
                                         Long categoryId, Long gradeId, Long subjectId) {
        categoryValidationService.validateFilter(gradeId, subjectId, categoryId);
        if (filterSnapshotRepository.existsByUserIdAndName(userId, name)) {
            throw new IllegalArgumentException("快照名称已存在，请更换名称");
        }
        FilterSnapshot snapshot = new FilterSnapshot();
        snapshot.setUserId(userId);
        snapshot.setName(name);
        snapshot.setKeyword(keyword != null ? keyword : "");
        snapshot.setCategoryId(categoryId);
        snapshot.setGradeId(gradeId);
        snapshot.setSubjectId(subjectId);
        snapshot.setSort(0);
        return filterSnapshotRepository.save(snapshot);
    }

    public FilterSnapshot updateSnapshot(Long id, Long userId, String name, String keyword,
                                         Long categoryId, Long gradeId, Long subjectId) {
        categoryValidationService.validateFilter(gradeId, subjectId, categoryId);
        FilterSnapshot snapshot = filterSnapshotRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("快照不存在"));
        if (!snapshot.getName().equals(name) && filterSnapshotRepository.existsByUserIdAndName(userId, name)) {
            throw new IllegalArgumentException("快照名称已存在，请更换名称");
        }
        snapshot.setName(name);
        snapshot.setKeyword(keyword != null ? keyword : "");
        snapshot.setCategoryId(categoryId);
        snapshot.setGradeId(gradeId);
        snapshot.setSubjectId(subjectId);
        return filterSnapshotRepository.save(snapshot);
    }

    @Transactional
    public boolean deleteSnapshot(Long id, Long userId) {
        if (!filterSnapshotRepository.existsById(id)) {
            return false;
        }
        filterSnapshotRepository.deleteByIdAndUserId(id, userId);
        return true;
    }
}
