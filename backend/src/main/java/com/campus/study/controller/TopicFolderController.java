package com.campus.study.controller;

import com.campus.study.common.Result;
import com.campus.study.entity.TopicFolder;
import com.campus.study.entity.TopicFolderItem;
import com.campus.study.service.TopicFolderService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/topic-folders")
public class TopicFolderController {

    @Resource
    private TopicFolderService topicFolderService;

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<TopicFolder> folderPage = topicFolderService.getFolderPage(page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("list", folderPage.getContent());
        result.put("total", folderPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        return Result.success(result);
    }

    @GetMapping("/all")
    public Result<List<TopicFolder>> all() {
        List<TopicFolder> list = topicFolderService.getAllFolders();
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<TopicFolder> detail(@PathVariable Long id) {
        TopicFolder folder = topicFolderService.getFolderDetailWithViewCount(id);
        if (folder == null) {
            return Result.error("专题资料夹不存在");
        }
        if (folder.getStatus() != 1) {
            return Result.error("专题资料夹已下架");
        }
        return Result.success(folder);
    }

    @GetMapping("/{id}/items")
    public Result<List<TopicFolderItem>> items(@PathVariable Long id) {
        TopicFolder folder = topicFolderService.getFolderById(id);
        if (folder == null) {
            return Result.error("专题资料夹不存在");
        }
        if (folder.getStatus() != 1) {
            return Result.error("专题资料夹已下架");
        }
        List<TopicFolderItem> items = topicFolderService.getFolderItems(id);
        return Result.success(items);
    }

    @PostMapping
    public Result<TopicFolder> create(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Long userId,
            @RequestParam(required = false) String cover,
            @RequestBody(required = false) Map<String, Object> body) {
        if (name == null || name.trim().isEmpty()) {
            return Result.error("请输入资料夹名称");
        }
        List<Map<String, Object>> items = null;
        if (body != null && body.containsKey("items")) {
            items = (List<Map<String, Object>>) body.get("items");
        }
        TopicFolder folder = topicFolderService.createFolder(name.trim(), description, userId, cover, items);
        return Result.success("创建成功", folder);
    }

    @PutMapping("/{id}")
    public Result<TopicFolder> update(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String cover,
            @RequestParam Long userId,
            @RequestBody(required = false) Map<String, Object> body) {
        List<Map<String, Object>> items = null;
        if (body != null && body.containsKey("items")) {
            items = (List<Map<String, Object>>) body.get("items");
        }
        TopicFolder folder = topicFolderService.updateFolder(id, name, description, cover, items, userId);
        if (folder == null) {
            return Result.error("操作失败，资料夹不存在或无权操作");
        }
        return Result.success("更新成功", folder);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @RequestParam Long userId) {
        boolean success = topicFolderService.deleteFolder(id, userId);
        if (!success) {
            return Result.error("操作失败，资料夹不存在或无权操作");
        }
        return Result.success("删除成功", null);
    }

    @GetMapping("/my")
    public Result<Map<String, Object>> myFolders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Long userId) {
        Page<TopicFolder> folderPage = topicFolderService.getMyFolders(userId, page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("list", folderPage.getContent());
        result.put("total", folderPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        return Result.success(result);
    }
}
