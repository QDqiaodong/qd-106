package com.campus.study.controller;

import com.campus.study.common.Result;
import com.campus.study.entity.FilterSnapshot;
import com.campus.study.service.FilterSnapshotService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/filter-snapshots")
public class FilterSnapshotController {

    @Resource
    private FilterSnapshotService filterSnapshotService;

    @GetMapping
    public Result<List<FilterSnapshot>> mySnapshots(@RequestParam Long userId) {
        List<FilterSnapshot> list = filterSnapshotService.getMySnapshots(userId);
        return Result.success(list);
    }

    @PostMapping
    public Result<FilterSnapshot> create(
            @RequestParam Long userId,
            @RequestParam String name,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long gradeId,
            @RequestParam(required = false) Long subjectId) {
        try {
            FilterSnapshot snapshot = filterSnapshotService.createSnapshot(
                    userId, name, keyword, categoryId, gradeId, subjectId);
            return Result.success("保存成功", snapshot);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<FilterSnapshot> update(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam String name,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long gradeId,
            @RequestParam(required = false) Long subjectId) {
        try {
            FilterSnapshot snapshot = filterSnapshotService.updateSnapshot(
                    id, userId, name, keyword, categoryId, gradeId, subjectId);
            return Result.success("更新成功", snapshot);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @RequestParam Long userId) {
        boolean success = filterSnapshotService.deleteSnapshot(id, userId);
        if (!success) {
            return Result.error("快照不存在");
        }
        return Result.success("删除成功", null);
    }
}
