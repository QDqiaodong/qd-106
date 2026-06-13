package com.campus.study.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class FileUtil {

    @Value("${file.upload.path:/app/uploads}")
    private String uploadPath;

    @Value("${file.upload.max-size:10485760}")
    private long maxFileSize;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt",
            "jpg", "jpeg", "png", "gif"
    );

    public String uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(originalFilename);
        if (!isValidExtension(extension)) {
            throw new IllegalArgumentException("不支持的文件格式，支持的格式: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("文件大小超过限制，最大支持: " + (maxFileSize / 1024 / 1024) + "MB");
        }

        File directory = new File(uploadPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String newFilename = UUID.randomUUID().toString() + "." + extension;
        File destFile = new File(uploadPath, newFilename);
        file.transferTo(destFile);

        return "/uploads/" + newFilename;
    }

    public String uploadFile(MultipartFile file, String subDirectory) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(originalFilename);
        if (!isValidExtension(extension)) {
            throw new IllegalArgumentException("不支持的文件格式，支持的格式: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("文件大小超过限制，最大支持: " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String fullPath = uploadPath + File.separator + subDirectory;
        File directory = new File(fullPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String newFilename = UUID.randomUUID().toString() + "." + extension;
        File destFile = new File(fullPath, newFilename);
        file.transferTo(destFile);

        return "/uploads/" + subDirectory + "/" + newFilename;
    }

    public boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }

        String relativePath = filePath.startsWith("/uploads/") ? filePath.substring(9) : filePath;
        File file = new File(uploadPath, relativePath);
        return file.exists() && file.delete();
    }

    public boolean isValidExtension(String extension) {
        if (extension == null) {
            return false;
        }
        return ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
    }

    public String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    public List<String> getAllowedExtensions() {
        return ALLOWED_EXTENSIONS;
    }

    public String calculateFileHash(MultipartFile file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = file.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("哈希算法不可用: " + e.getMessage(), e);
        }
    }

    public String calculateFileHash(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("哈希算法不可用: " + e.getMessage(), e);
        }
    }

    public String calculateFileHash(byte[] bytes) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(bytes);
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("哈希算法不可用: " + e.getMessage(), e);
        }
    }
}
