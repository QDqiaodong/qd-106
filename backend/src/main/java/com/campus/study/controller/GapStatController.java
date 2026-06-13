package com.campus.study.controller;

import com.campus.study.common.Result;
import com.campus.study.service.GapStatService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/stats")
public class GapStatController {

    @Resource
    private GapStatService gapStatService;

    @GetMapping("/gap")
    public Result<Map<String, Object>> gapStatistics() {
        Map<String, Object> data = gapStatService.getGapStatistics();
        return Result.success(data);
    }
}
