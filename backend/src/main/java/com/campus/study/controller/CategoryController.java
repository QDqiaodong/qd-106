package com.campus.study.controller;

import com.campus.study.common.Result;
import com.campus.study.entity.Category;
import com.campus.study.service.CategoryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    @GetMapping
    public Result<List<Category>> list() {
        List<Category> list = categoryService.getEnabledList();
        return Result.success(list);
    }
}
