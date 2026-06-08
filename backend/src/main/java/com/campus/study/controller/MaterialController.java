package com.campus.study.controller;

import com.campus.study.common.Result;
import com.campus.study.entity.Material;
import com.campus.study.service.FavoriteService;
import com.campus.study.service.MaterialService;
import com.campus.study.util.FileUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/materials")
public class MaterialController {

    @Resource
    private MaterialService materialService;

    @Resource
    private FavoriteService favoriteService;

    @Resource
    private FileUtil fileUtil;

    @Value("${file.upload.path:/app/uploads}")
    private String uploadPath;

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long gradeId,
            @RequestParam(required = false) Long subjectId) {
        Page<Material> materialPage = materialService.getMaterialPage(page, size, keyword, categoryId, gradeId, subjectId);

        Map<String, Object> result = new HashMap<>();
        result.put("list", materialPage.getContent());
        result.put("total", materialPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Transactional
    public Result<Material> detail(@PathVariable Long id) {
        Material material = materialService.getMaterialById(id);
        if (material == null) {
            return Result.error("资料不存在");
        }
        if (material.getStatus() != 1) {
            return Result.error("资料已下架");
        }
        materialService.incrementViewCount(id);
        material.setViewCount(material.getViewCount() + 1);
        return Result.success(material);
    }

    @PostMapping
    public Result<Material> upload(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long gradeId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam Long userId,
            @RequestParam(required = false) MultipartFile cover,
            @RequestParam MultipartFile file) {
        try {
            Material material = new Material();
            material.setTitle(title);
            material.setDescription(description != null ? description : "");
            material.setCategoryId(categoryId != null ? categoryId : 0L);
            material.setGradeId(gradeId != null ? gradeId : 0L);
            material.setSubjectId(subjectId != null ? subjectId : 0L);

            if (cover != null && !cover.isEmpty()) {
                String coverUrl = fileUtil.uploadFile(cover, "covers");
                material.setCover(coverUrl);
            }

            String fileUrl = fileUtil.uploadFile(file, "materials");
            material.setFileUrl(fileUrl);
            material.setFileSize(file.getSize());

            Material saved = materialService.uploadMaterial(material, userId);
            return Result.success("上传成功", saved);
        } catch (IOException e) {
            return Result.error("文件上传失败: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @RequestParam Long userId) {
        Material material = materialService.getMaterialById(id);
        if (material == null) {
            return Result.error("资料不存在");
        }
        boolean success = materialService.deleteMaterial(id, userId);
        if (!success) {
            return Result.error("无权操作");
        }
        return Result.success("下架成功", null);
    }

    @GetMapping("/hot")
    public Result<List<Material>> hot() {
        List<Material> list = materialService.getHotMaterials();
        return Result.success(list);
    }

    @GetMapping("/my")
    public Result<Map<String, Object>> myUploads(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Long userId) {
        Page<Material> materialPage = materialService.getMyUploads(page, size, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("list", materialPage.getContent());
        result.put("total", materialPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    @PostMapping("/{id}/favorite")
    public Result<Void> favorite(@PathVariable Long id, @RequestParam Long userId) {
        Material material = materialService.getMaterialById(id);
        if (material == null) {
            return Result.error("资料不存在");
        }
        boolean success = favoriteService.addFavorite(userId, id);
        if (!success) {
            return Result.error("已收藏");
        }
        return Result.success("收藏成功", null);
    }

    @DeleteMapping("/{id}/favorite")
    public Result<Void> unfavorite(@PathVariable Long id, @RequestParam Long userId) {
        boolean success = favoriteService.removeFavorite(userId, id);
        if (!success) {
            return Result.error("未收藏");
        }
        return Result.success("取消收藏成功", null);
    }

    @GetMapping("/{id}/preview")
    public void preview(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Material material = materialService.getMaterialById(id);
        if (material == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (material.getStatus() != 1) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String fileUrl = material.getFileUrl();
        String relativePath = fileUrl.startsWith("/uploads/") ? fileUrl.substring(9) : fileUrl;
        File file = new File(uploadPath, relativePath);

        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String fileName = material.getTitle() + "." + fileUtil.getFileExtension(file.getName());
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.setContentLengthLong(file.length());

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        }
    }
}
