package com.campus.study.controller;

import com.campus.study.common.Result;
import com.campus.study.entity.Grade;
import com.campus.study.service.GradeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/grades")
public class GradeController {

    @Resource
    private GradeService gradeService;

    @GetMapping
    public Result<List<Grade>> list() {
        List<Grade> list = gradeService.getEnabledList();
        return Result.success(list);
    }
}
