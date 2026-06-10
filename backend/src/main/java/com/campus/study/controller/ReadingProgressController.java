package com.campus.study.controller;

import com.campus.study.common.Result;
import com.campus.study.entity.ReadingProgress;
import com.campus.study.service.MaterialService;
import com.campus.study.service.ReadingProgressService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/materials/{materialId}/progress")
public class ReadingProgressController {

    @Resource
    private ReadingProgressService readingProgressService;

    @Resource
    private MaterialService materialService;

    @GetMapping
    public Result<ReadingProgress> get(@PathVariable Long materialId, @RequestParam Long userId) {
        if (materialService.getMaterialById(materialId) == null) {
            return Result.error("资料不存在");
        }
        ReadingProgress progress = readingProgressService.getProgress(userId, materialId);
        return Result.success(progress);
    }

    @PostMapping
    public Result<ReadingProgress> save(
            @PathVariable Long materialId,
            @RequestParam Long userId,
            @RequestParam Integer pageNumber) {
        if (materialService.getMaterialById(materialId) == null) {
            return Result.error("资料不存在");
        }
        if (pageNumber == null || pageNumber <= 0) {
            return Result.error("页码必须大于0");
        }
        ReadingProgress progress = readingProgressService.saveProgress(userId, materialId, pageNumber);
        return Result.success("保存成功", progress);
    }

    @DeleteMapping
    public Result<Void> delete(@PathVariable Long materialId, @RequestParam Long userId) {
        boolean success = readingProgressService.deleteProgress(userId, materialId);
        if (!success) {
            return Result.error("阅读进度不存在");
        }
        return Result.success("删除成功", null);
    }
}
