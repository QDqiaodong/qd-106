package com.campus.study.controller;

import com.campus.study.common.Result;
import com.campus.study.entity.Bookmark;
import com.campus.study.service.BookmarkService;
import com.campus.study.service.MaterialService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/materials/{materialId}/bookmarks")
public class BookmarkController {

    @Resource
    private BookmarkService bookmarkService;

    @Resource
    private MaterialService materialService;

    @GetMapping
    public Result<List<Bookmark>> list(@PathVariable Long materialId, @RequestParam Long userId) {
        if (materialService.getMaterialById(materialId) == null) {
            return Result.error("资料不存在");
        }
        List<Bookmark> list = bookmarkService.getBookmarks(userId, materialId);
        return Result.success(list);
    }

    @PostMapping
    public Result<Bookmark> add(
            @PathVariable Long materialId,
            @RequestParam Long userId,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) String chapterName,
            @RequestParam(required = false) String note) {
        if (materialService.getMaterialById(materialId) == null) {
            return Result.error("资料不存在");
        }
        if ((pageNumber == null || pageNumber <= 0) && (chapterName == null || chapterName.trim().isEmpty())) {
            return Result.error("请填写页码或章节名称");
        }
        Bookmark bookmark = bookmarkService.addBookmark(userId, materialId, pageNumber, chapterName, note);
        return Result.success("添加书签成功", bookmark);
    }

    @PutMapping("/{id}")
    public Result<Bookmark> update(
            @PathVariable Long materialId,
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) String chapterName,
            @RequestParam(required = false) String note) {
        Bookmark bookmark = bookmarkService.updateBookmark(id, userId, pageNumber, chapterName, note);
        if (bookmark == null) {
            return Result.error("书签不存在");
        }
        return Result.success("更新成功", bookmark);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long materialId, @PathVariable Long id, @RequestParam Long userId) {
        boolean success = bookmarkService.deleteBookmark(id, userId);
        if (!success) {
            return Result.error("书签不存在");
        }
        return Result.success("删除成功", null);
    }
}
