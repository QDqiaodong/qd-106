package com.campus.study.controller;

import com.campus.study.common.Result;
import com.campus.study.service.KeywordFrequencyService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/keyword-index")
public class KeywordFrequencyController {

    @Resource
    private KeywordFrequencyService keywordFrequencyService;

    @GetMapping("/trending")
    public Result<List<Map<String, Object>>> trending(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(defaultValue = "20") int limit) {
        if (subjectId != null && subjectId > 0) {
            return Result.success(keywordFrequencyService.getTrendingBySubject(subjectId, limit));
        }
        return Result.success(keywordFrequencyService.getTrendingGlobal(limit));
    }

    @GetMapping("/recent")
    public Result<List<Map<String, Object>>> recent(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "20") int limit) {
        if (days <= 0) days = 7;
        if (days > 90) days = 90;
        if (limit <= 0) limit = 20;
        if (limit > 100) limit = 100;
        return Result.success(keywordFrequencyService.getRecentTrending(subjectId, days, limit));
    }

    @GetMapping("/subject-overview")
    public Result<Map<String, Object>> subjectOverview() {
        return Result.success(keywordFrequencyService.getSubjectTrendingOverview());
    }

    @PostMapping("/rebuild")
    public Result<Void> rebuildIndex() {
        keywordFrequencyService.rebuildIndex();
        return Result.success("索引重建完成", null);
    }
}
