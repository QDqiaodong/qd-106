package com.campus.study.controller;

import com.campus.study.common.Result;
import com.campus.study.service.FavoriteService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/favorites")
public class FavoriteController {

    @Resource
    private FavoriteService favoriteService;

    @GetMapping
    public Result<Map<String, Object>> myFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Long userId) {
        Map<String, Object> result = favoriteService.getMyFavorites(page, size, userId);
        return Result.success(result);
    }

    @PutMapping("/{materialId}/review-status")
    public Result<Void> updateReviewStatus(
            @PathVariable Long materialId,
            @RequestParam Long userId,
            @RequestParam Integer status) {
        boolean success = favoriteService.updateReviewStatus(userId, materialId, status);
        if (!success) {
            return Result.error("更新失败");
        }
        return Result.success("更新成功", null);
    }

    @GetMapping("/{materialId}/review-status")
    public Result<Integer> getReviewStatus(
            @PathVariable Long materialId,
            @RequestParam Long userId) {
        Integer status = favoriteService.getReviewStatus(userId, materialId);
        return Result.success(status);
    }
}
