package com.campus.study.controller;

import com.campus.study.common.Result;
import com.campus.study.entity.Correction;
import com.campus.study.service.CorrectionService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/corrections")
public class CorrectionController {

    @Resource
    private CorrectionService correctionService;

    @PostMapping
    public Result<Correction> submit(
            @RequestParam Long materialId,
            @RequestParam Long userId,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam String errorDescription,
            @RequestParam(required = false) String correctionSuggestion) {
        try {
            Correction saved = correctionService.submitCorrection(
                    materialId, userId, pageNumber, errorDescription, correctionSuggestion);
            return Result.success("提交成功", saved);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/material/{materialId}")
    public Result<Map<String, Object>> listByMaterial(
            @PathVariable Long materialId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Correction> correctionPage = correctionService.getCorrectionsByMaterial(materialId, page, size);

        Map<String, Object> result = new HashMap<>();
        result.put("list", correctionPage.getContent());
        result.put("total", correctionPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    @GetMapping("/my")
    public Result<Map<String, Object>> myCorrections(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Correction> correctionPage = correctionService.getMyCorrections(userId, page, size);

        Map<String, Object> result = new HashMap<>();
        result.put("list", correctionPage.getContent());
        result.put("total", correctionPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    @GetMapping("/uploader")
    public Result<Map<String, Object>> uploaderCorrections(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        Page<Correction> correctionPage = correctionService.getPendingCorrectionsForUploader(userId, page, size, status);

        Map<String, Object> result = new HashMap<>();
        result.put("list", correctionPage.getContent());
        result.put("total", correctionPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    @GetMapping("/uploader/stats")
    public Result<Map<String, Long>> uploaderStats(@RequestParam Long userId) {
        Map<String, Long> stats = correctionService.getCorrectionStatsForUploader(userId);
        return Result.success(stats);
    }

    @PutMapping("/{id}/handle")
    public Result<Correction> handle(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam int status,
            @RequestParam(required = false) String handleRemark) {
        try {
            Correction handled = correctionService.handleCorrection(id, userId, status, handleRemark);
            return Result.success(status == 1 ? "已采纳" : "已驳回", handled);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}
