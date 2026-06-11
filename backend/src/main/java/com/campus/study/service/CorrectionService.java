package com.campus.study.service;

import com.campus.study.entity.Correction;
import com.campus.study.entity.Material;
import com.campus.study.entity.User;
import com.campus.study.repository.CorrectionRepository;
import com.campus.study.repository.MaterialRepository;
import com.campus.study.repository.UserRepository;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CorrectionService {

    @Resource
    private CorrectionRepository correctionRepository;

    @Resource
    private MaterialRepository materialRepository;

    @Resource
    private UserRepository userRepository;

    public Correction submitCorrection(Long materialId, Long userId, Integer pageNumber,
                                       String errorDescription, String correctionSuggestion) {
        Material material = materialRepository.findById(materialId).orElse(null);
        if (material == null) {
            throw new IllegalArgumentException("资料不存在");
        }
        if (material.getStatus() != 1) {
            throw new IllegalArgumentException("资料已下架");
        }
        if (errorDescription == null || errorDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("请填写错误说明");
        }

        Correction correction = new Correction();
        correction.setMaterialId(materialId);
        correction.setUserId(userId);
        correction.setPageNumber(pageNumber);
        correction.setErrorDescription(errorDescription.trim());
        correction.setCorrectionSuggestion(correctionSuggestion != null ? correctionSuggestion.trim() : "");
        correction.setStatus(0);

        return correctionRepository.save(correction);
    }

    public Page<Correction> getCorrectionsByMaterial(Long materialId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Correction> resultPage = correctionRepository.findByMaterialId(materialId, pageable);
        enrichCorrections(resultPage.getContent());
        return resultPage;
    }

    public Page<Correction> getMyCorrections(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Correction> resultPage = correctionRepository.findByUserId(userId, pageable);
        enrichCorrections(resultPage.getContent());
        return resultPage;
    }

    public Page<Correction> getPendingCorrectionsForUploader(Long uploaderUserId, int page, int size, Integer status) {
        List<Material> myMaterials = materialRepository.findByUserId(uploaderUserId);
        List<Long> materialIds = myMaterials.stream().map(Material::getId).collect(Collectors.toList());

        if (materialIds.isEmpty()) {
            return Page.empty();
        }

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Correction> resultPage;

        if (status != null && status >= 0) {
            resultPage = correctionRepository.findByMaterialIdInAndStatus(materialIds, status, pageable);
        } else {
            resultPage = correctionRepository.findByMaterialIdIn(materialIds, pageable);
        }

        enrichCorrections(resultPage.getContent());
        return resultPage;
    }

    public Map<String, Long> getCorrectionStatsForUploader(Long uploaderUserId) {
        List<Material> myMaterials = materialRepository.findByUserId(uploaderUserId);
        List<Long> materialIds = myMaterials.stream().map(Material::getId).collect(Collectors.toList());

        Map<String, Long> stats = new java.util.HashMap<>();
        if (materialIds.isEmpty()) {
            stats.put("pending", 0L);
            stats.put("accepted", 0L);
            stats.put("rejected", 0L);
            stats.put("total", 0L);
            return stats;
        }

        long pending = correctionRepository.countByMaterialIdInAndStatus(materialIds, 0);
        long accepted = correctionRepository.countByMaterialIdInAndStatus(materialIds, 1);
        long rejected = correctionRepository.countByMaterialIdInAndStatus(materialIds, 2);

        stats.put("pending", pending);
        stats.put("accepted", accepted);
        stats.put("rejected", rejected);
        stats.put("total", pending + accepted + rejected);
        return stats;
    }

    @Transactional
    public Correction handleCorrection(Long correctionId, Long handlerUserId, int status, String handleRemark) {
        Correction correction = correctionRepository.findById(correctionId).orElse(null);
        if (correction == null) {
            throw new IllegalArgumentException("勘误单不存在");
        }
        if (correction.getStatus() != 0) {
            throw new IllegalArgumentException("该勘误单已处理");
        }
        if (status != 1 && status != 2) {
            throw new IllegalArgumentException("无效的处理状态");
        }

        Material material = materialRepository.findById(correction.getMaterialId()).orElse(null);
        if (material == null || !material.getUserId().equals(handlerUserId)) {
            throw new IllegalArgumentException("无权处理该勘误单");
        }

        correction.setStatus(status);
        correction.setHandlerId(handlerUserId);
        correction.setHandleRemark(handleRemark != null ? handleRemark.trim() : "");
        correction.setHandledAt(LocalDateTime.now());

        return correctionRepository.save(correction);
    }

    private void enrichCorrections(List<Correction> corrections) {
        if (corrections == null || corrections.isEmpty()) {
            return;
        }

        List<Long> materialIds = corrections.stream()
                .map(Correction::getMaterialId)
                .distinct()
                .collect(Collectors.toList());
        List<Long> userIds = corrections.stream()
                .map(Correction::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> materialTitleMap = materialRepository.findAllById(materialIds).stream()
                .collect(Collectors.toMap(Material::getId, Material::getTitle));
        Map<Long, String> userNicknameMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u ->
                        u.getNickname() != null && !u.getNickname().isEmpty() ? u.getNickname() : u.getUsername()));

        for (Correction correction : corrections) {
            correction.setMaterialTitle(materialTitleMap.getOrDefault(correction.getMaterialId(), "未知资料"));
            correction.setSubmitterNickname(userNicknameMap.getOrDefault(correction.getUserId(), "未知用户"));
        }
    }
}
