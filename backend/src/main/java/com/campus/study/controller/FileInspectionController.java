package com.campus.study.controller;

import com.campus.study.common.Result;
import com.campus.study.entity.FileInspectionRecord;
import com.campus.study.service.FileInspectionService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inspection")
public class FileInspectionController {

    @Resource
    private FileInspectionService fileInspectionService;

    @PostMapping("/run/full")
    public Result<Map<String, Object>> runFullInspection() {
        try {
            Map<String, Object> result = fileInspectionService.runFullInspection();
            return Result.success("全量巡检任务已执行", result);
        } catch (Exception e) {
            return Result.error("全量巡检执行失败: " + e.getMessage());
        }
    }

    @PostMapping("/run/incremental")
    public Result<Map<String, Object>> runIncrementalInspection(
            @RequestParam(defaultValue = "100") int limit) {
        try {
            Map<String, Object> result = fileInspectionService.runIncrementalInspection(limit);
            return Result.success("增量巡检任务已执行", result);
        } catch (Exception e) {
            return Result.error("增量巡检执行失败: " + e.getMessage());
        }
    }

    @PostMapping("/run/material/{materialId}")
    public Result<Map<String, Object>> inspectMaterial(@PathVariable Long materialId) {
        try {
            var exceptions = fileInspectionService.inspectSingleMaterial(materialId);
            Map<String, Object> result = new HashMap<>();
            result.put("materialId", materialId);
            result.put("exceptionCount", exceptions.size());
            result.put("exceptions", exceptions.stream()
                    .map(e -> Map.of("code", e.getCode(), "name", e.getName()))
                    .toList());
            return Result.success("单资料巡检完成", result);
        } catch (Exception e) {
            return Result.error("单资料巡检执行失败: " + e.getMessage());
        }
    }

    @GetMapping("/records")
    public Result<Map<String, Object>> getRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer handleStatus,
            @RequestParam(required = false) Integer exceptionType,
            @RequestParam(required = false) String materialTitle,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        try {
            Page<FileInspectionRecord> recordPage = fileInspectionService.getRecordsPage(
                    page, size, handleStatus, exceptionType, materialTitle, startTime, endTime);
            Map<String, Object> result = new HashMap<>();
            result.put("list", recordPage.getContent());
            result.put("total", recordPage.getTotalElements());
            result.put("page", page);
            result.put("size", size);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询巡检记录失败: " + e.getMessage());
        }
    }

    @GetMapping("/records/{id}")
    public Result<FileInspectionRecord> getRecordDetail(@PathVariable Long id) {
        FileInspectionRecord record = fileInspectionService.getRecordById(id);
        if (record == null) {
            return Result.error("巡检记录不存在");
        }
        return Result.success(record);
    }

    @GetMapping("/records/material/{materialId}")
    public Result<Map<String, Object>> getRecordsByMaterial(
            @PathVariable Long materialId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<FileInspectionRecord> recordPage = fileInspectionService.getRecordsByMaterial(materialId, page, size);
            Map<String, Object> result = new HashMap<>();
            result.put("list", recordPage.getContent());
            result.put("total", recordPage.getTotalElements());
            result.put("page", page);
            result.put("size", size);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询资料巡检记录失败: " + e.getMessage());
        }
    }

    @PutMapping("/records/{id}/status")
    public Result<Void> updateHandleStatus(
            @PathVariable Long id,
            @RequestParam Integer handleStatus,
            @RequestParam(required = false) Long handlerId,
            @RequestParam(required = false) String handleRemark) {
        boolean success = fileInspectionService.updateHandleStatus(id, handleStatus, handlerId, handleRemark);
        if (!success) {
            return Result.error("更新处理状态失败");
        }
        return Result.success("处理状态已更新", null);
    }

    @PostMapping("/records/batch/resolve")
    public Result<Map<String, Object>> batchResolve(
            @RequestBody Map<String, Object> request) {
        try {
            List<Number> idsList = (List<Number>) request.get("ids");
            Long[] ids = idsList.stream().map(Number::longValue).toArray(Long[]::new);
            Long handlerId = request.get("handlerId") != null
                    ? ((Number) request.get("handlerId")).longValue() : null;
            String remark = (String) request.get("remark");
            int count = fileInspectionService.batchResolve(ids, handlerId, remark);
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", count);
            result.put("totalCount", ids.length);
            return Result.success("批量标记已解决完成", result);
        } catch (Exception e) {
            return Result.error("批量标记已解决失败: " + e.getMessage());
        }
    }

    @PostMapping("/records/batch/ignore")
    public Result<Map<String, Object>> batchIgnore(
            @RequestBody Map<String, Object> request) {
        try {
            List<Number> idsList = (List<Number>) request.get("ids");
            Long[] ids = idsList.stream().map(Number::longValue).toArray(Long[]::new);
            Long handlerId = request.get("handlerId") != null
                    ? ((Number) request.get("handlerId")).longValue() : null;
            String remark = (String) request.get("remark");
            int count = fileInspectionService.batchIgnore(ids, handlerId, remark);
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", count);
            result.put("totalCount", ids.length);
            return Result.success("批量忽略完成", result);
        } catch (Exception e) {
            return Result.error("批量忽略失败: " + e.getMessage());
        }
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = fileInspectionService.getStatistics();
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取统计数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/exception-types")
    public Result<List<Map<String, Object>>> getExceptionTypes() {
        return Result.success(fileInspectionService.getExceptionTypeList());
    }

    @GetMapping("/handle-statuses")
    public Result<List<Map<String, Object>>> getHandleStatuses() {
        return Result.success(fileInspectionService.getHandleStatusList());
    }
}
