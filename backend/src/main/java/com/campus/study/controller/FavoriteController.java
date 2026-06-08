package com.campus.study.controller;

import com.campus.study.common.Result;
import com.campus.study.service.FavoriteService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
