package com.campus.study.service;

import com.campus.study.entity.ReadingProgress;
import com.campus.study.repository.ReadingProgressRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ReadingProgressService {

    @Resource
    private ReadingProgressRepository readingProgressRepository;

    public ReadingProgress getProgress(Long userId, Long materialId) {
        return readingProgressRepository.findByUserIdAndMaterialId(userId, materialId).orElse(null);
    }

    @Transactional
    public ReadingProgress saveProgress(Long userId, Long materialId, Integer pageNumber) {
        Optional<ReadingProgress> optional = readingProgressRepository.findByUserIdAndMaterialId(userId, materialId);
        ReadingProgress progress;
        if (optional.isPresent()) {
            progress = optional.get();
            progress.setPageNumber(pageNumber);
            progress.setLastReadAt(LocalDateTime.now());
        } else {
            progress = new ReadingProgress();
            progress.setUserId(userId);
            progress.setMaterialId(materialId);
            progress.setPageNumber(pageNumber);
        }
        return readingProgressRepository.save(progress);
    }

    @Transactional
    public boolean deleteProgress(Long userId, Long materialId) {
        Optional<ReadingProgress> optional = readingProgressRepository.findByUserIdAndMaterialId(userId, materialId);
        if (optional.isEmpty()) {
            return false;
        }
        readingProgressRepository.deleteByUserIdAndMaterialId(userId, materialId);
        return true;
    }
}
