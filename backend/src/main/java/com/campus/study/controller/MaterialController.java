package com.campus.study.controller;

import com.campus.study.common.Result;
import com.campus.study.entity.Material;
import com.campus.study.service.CategoryValidationService;
import com.campus.study.service.ChunkUploadService;
import com.campus.study.service.FavoriteService;
import com.campus.study.service.MaterialFingerprintService;
import com.campus.study.service.MaterialService;
import com.campus.study.service.ReadingProgressService;
import com.campus.study.util.FileUtil;
import com.campus.study.util.IpUtil;
import com.campus.study.vo.DuplicateCheckResult;
import com.campus.study.vo.MaterialThumbnailVO;
import com.campus.study.vo.PreviewStatusVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
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
    private ReadingProgressService readingProgressService;

    @Resource
    private FileUtil fileUtil;

    @Resource
    private ChunkUploadService chunkUploadService;

    @Resource
    private CategoryValidationService categoryValidationService;

    @Resource
    private MaterialFingerprintService materialFingerprintService;

    @Value("${file.upload.path:/app/uploads}")
    private String uploadPath;

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long gradeId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long userId) {
        try {
            categoryValidationService.validateFilter(gradeId, subjectId, categoryId);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
        Page<Material> materialPage = materialService.getMaterialPage(page, size, keyword, categoryId, gradeId, subjectId);

        List<Material> list = materialPage.getContent();
        if (userId != null) {
            for (Material material : list) {
                boolean favorited = favoriteService.isFavorite(userId, material.getId());
                material.setFavorited(favorited);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", materialPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<Material> detail(@PathVariable Long id,
                                    @RequestParam(required = false) Long userId,
                                    HttpServletRequest request) {
        Material material = materialService.getMaterialByIdWithViewCount(id);
        if (material == null) {
            return Result.error("资料不存在");
        }
        if (material.getStatus() != 1) {
            return Result.error("资料已下架");
        }

        String ip = IpUtil.getIpAddr(request);
        String userAgent = request.getHeader("User-Agent");
        boolean isNewView = materialService.recordView(id, userId, ip, userAgent);
        if (isNewView) {
            material.setViewCount(material.getViewCount() + 1);
        }

        if (userId != null) {
            boolean favorited = favoriteService.isFavorite(userId, id);
            material.setFavorited(favorited);
            material.setReadingProgress(readingProgressService.getProgress(userId, id));
        }

        return Result.success(material);
    }

    @PostMapping("/check-duplicate")
    public Result<DuplicateCheckResult> checkDuplicate(
            @RequestParam String title,
            @RequestParam(required = false) Long fileSize,
            @RequestParam(required = false) String fileMd5,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) MultipartFile file) {
        try {
            DuplicateCheckResult result;
            if (file != null && !file.isEmpty()) {
                result = materialFingerprintService.checkDuplicate(title, file.getSize(), file, userId);
            } else {
                result = materialFingerprintService.checkDuplicate(title, fileSize, fileMd5, userId);
            }
            return Result.success(result);
        } catch (IOException e) {
            return Result.error("重复检测失败: " + e.getMessage());
        }
    }

    @PostMapping
    public Result<Material> upload(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long gradeId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer totalPages,
            @RequestParam Long userId,
            @RequestParam(required = false) MultipartFile cover,
            @RequestParam(required = false) Boolean forceUpload,
            @RequestParam MultipartFile file) {
        try {
            categoryValidationService.validateUpload(gradeId, subjectId, categoryId, title);

            if (forceUpload == null || !forceUpload) {
                DuplicateCheckResult duplicateCheck = materialFingerprintService.checkDuplicate(
                        title, file.getSize(), file, userId);
                if (duplicateCheck.isDuplicate()) {
                    return Result.error(409, duplicateCheck.getWarningMessage(), null);
                }
            }

            Material material = new Material();
            material.setTitle(title);
            material.setDescription(description != null ? description : "");
            material.setCategoryId(categoryId != null ? categoryId : 0L);
            material.setGradeId(gradeId != null ? gradeId : 0L);
            material.setSubjectId(subjectId != null ? subjectId : 0L);
            material.setTotalPages(totalPages != null ? totalPages : 0);

            if (cover != null && !cover.isEmpty()) {
                String coverUrl = fileUtil.uploadFile(cover, "covers");
                material.setCover(coverUrl);
            }

            String fileUrl = fileUtil.uploadFile(file, "materials");
            material.setFileUrl(fileUrl);
            material.setFileSize(file.getSize());

            materialFingerprintService.saveMaterialHash(material, file);

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
    public Result<?> hot(@RequestParam(required = false) String range) {
        if ("7d".equals(range)) {
            return Result.success(materialService.getHotMaterials7Days());
        } else if ("30d".equals(range)) {
            return Result.success(materialService.getHotMaterials30Days());
        } else if ("semester".equals(range)) {
            return Result.success(materialService.getHotMaterialsSemester());
        }
        return Result.success(materialService.getAllHotMaterials());
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
        if (material.getStatus() == null || material.getStatus() != 1) {
            return Result.error("资料已下架，无法收藏");
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

    @GetMapping("/{id}/thumbnails")
    public Result<MaterialThumbnailVO> getThumbnails(
            @PathVariable Long id,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            if (limit <= 0) {
                limit = 5;
            }
            if (limit > 20) {
                limit = 20;
            }
            MaterialThumbnailVO vo = materialService.getMaterialThumbnails(id, limit);
            if (vo == null) {
                return Result.error("资料不存在或已下架");
            }
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error("获取缩略图失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/preview/status")
    public Result<PreviewStatusVO> previewStatus(@PathVariable Long id) {
        try {
            PreviewStatusVO status = materialService.checkPreviewStatus(id);
            return Result.success(status);
        } catch (Exception e) {
            return Result.error("预览状态查询失败: " + e.getMessage());
        }
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

    @PostMapping("/chunk/init")
    public Result<Map<String, Object>> initChunkUpload(
            @RequestParam String fileName,
            @RequestParam long fileSize,
            @RequestParam(defaultValue = "5242880") int chunkSize,
            @RequestParam(required = false) String fileMd5) {
        try {
            Map<String, Object> result = chunkUploadService.initUpload(fileName, fileSize, chunkSize, fileMd5);
            return Result.success("初始化成功", result);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/chunk/upload")
    public Result<Map<String, Object>> uploadChunk(
            @RequestParam String uploadId,
            @RequestParam int chunkIndex,
            @RequestParam MultipartFile chunk) {
        try {
            Map<String, Object> result = chunkUploadService.uploadChunk(uploadId, chunkIndex, chunk);
            return Result.success("分片上传成功", result);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (IOException e) {
            return Result.error("分片上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/chunk/status")
    public Result<Map<String, Object>> getChunkStatus(@RequestParam String uploadId) {
        Map<String, Object> result = chunkUploadService.getUploadStatus(uploadId);
        if (result == null) {
            return Result.error("上传任务不存在或已过期");
        }
        return Result.success(result);
    }

    @PostMapping("/chunk/check-duplicate")
    public Result<DuplicateCheckResult> checkChunkDuplicate(
            @RequestParam String uploadId,
            @RequestParam String title,
            @RequestParam Long userId) {
        try {
            DuplicateCheckResult result = chunkUploadService.checkDuplicateBeforeMerge(uploadId, title, userId);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (IOException e) {
            return Result.error("重复检测失败: " + e.getMessage());
        }
    }

    @PostMapping("/chunk/merge")
    public Result<Material> mergeChunks(
            @RequestParam String uploadId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long gradeId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam Long userId,
            @RequestParam(required = false) Boolean forceUpload) {
        try {
            categoryValidationService.validateUpload(gradeId, subjectId, categoryId, title);
            Material material = chunkUploadService.mergeChunks(
                    uploadId, title, description, categoryId, gradeId, subjectId, userId, forceUpload);
            return Result.success("上传成功", material);
        } catch (IllegalArgumentException e) {
            return Result.error(409, e.getMessage(), null);
        } catch (IOException e) {
            return Result.error("文件合并失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/chunk/cancel")
    public Result<Void> cancelChunkUpload(@RequestParam String uploadId) {
        boolean success = chunkUploadService.cancelUpload(uploadId);
        if (!success) {
            return Result.error("上传任务不存在或已过期");
        }
        return Result.success("已取消上传", null);
    }

    @GetMapping("/validation-rules")
    public Result<Map<String, Object>> getValidationRules() {
        return Result.success(categoryValidationService.getValidationRules());
    }

    @PostMapping("/{id}/download")
    public Result<Map<String, Object>> recordDownload(@PathVariable Long id) {
        Material material = materialService.getMaterialById(id);
        if (material == null) {
            return Result.error("资料不存在");
        }
        if (material.getStatus() != 1) {
            return Result.error("资料已下架");
        }
        int newCount = materialService.recordDownload(id);
        if (newCount < 0) {
            return Result.error("下载统计失败");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("downloadCount", newCount);
        result.put("materialId", id);
        return Result.success(result);
    }
}
