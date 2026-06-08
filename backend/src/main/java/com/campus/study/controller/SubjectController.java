package com.campus.study.controller;

import com.campus.study.common.Result;
import com.campus.study.entity.Subject;
import com.campus.study.service.SubjectService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/subjects")
public class SubjectController {

    @Resource
    private SubjectService subjectService;

    @GetMapping
    public Result<List<Subject>> list() {
        List<Subject> list = subjectService.getEnabledList();
        return Result.success(list);
    }
}
