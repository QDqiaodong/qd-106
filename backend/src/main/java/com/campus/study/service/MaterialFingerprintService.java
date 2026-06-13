package com.campus.study.service;

import com.campus.study.entity.Material;
import com.campus.study.repository.MaterialRepository;
import com.campus.study.util.FileUtil;
import com.campus.study.vo.DuplicateCheckResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MaterialFingerprintService {

    private static final double HASH_MATCH_THRESHOLD = 0.99;
    private static final double TITLE_SIMILARITY_THRESHOLD = 0.85;
    private static final double FILE_SIZE_TOLERANCE = 0.02;
    private static final double COMBINED_SCORE_THRESHOLD = 0.8;

    @Resource
    private MaterialRepository materialRepository;

    @Resource
    private FileUtil fileUtil;

    public DuplicateCheckResult checkDuplicate(String title, Long fileSize, MultipartFile file, Long excludeUserId) throws IOException {
        String fileHash = fileUtil.calculateFileHash(file);
        return checkDuplicate(title, fileSize, fileHash, excludeUserId);
    }

    public DuplicateCheckResult checkDuplicate(String title, Long fileSize, String fileHash, Long excludeUserId) {
        DuplicateCheckResult result = new DuplicateCheckResult();
        List<DuplicateCheckResult.MatchedMaterial> matchedMaterials = new ArrayList<>();

        if (fileHash != null && !fileHash.isEmpty()) {
            List<Material> hashMatches = materialRepository.findByFileHashAndStatus(fileHash, 1);
            for (Material mat : hashMatches) {
                if (excludeUserId != null && mat.getUserId().equals(excludeUserId)) {
                    continue;
                }
                double similarity = calculateCombinedScore(mat, title, fileSize, fileHash);
                String reason = "文件哈希完全匹配，内容高度一致";
                matchedMaterials.add(new DuplicateCheckResult.MatchedMaterial(mat, similarity, reason));
            }
        }

        if (fileSize != null && fileSize > 0) {
            long minSize = (long) (fileSize * (1 - FILE_SIZE_TOLERANCE));
            long maxSize = (long) (fileSize * (1 + FILE_SIZE_TOLERANCE));
            List<Material> sizeMatches = materialRepository.findByFileSizeBetweenAndStatus(minSize, maxSize, 1);
            for (Material mat : sizeMatches) {
                if (excludeUserId != null && mat.getUserId().equals(excludeUserId)) {
                    continue;
                }
                boolean alreadyMatched = matchedMaterials.stream()
                        .anyMatch(m -> m.getId().equals(mat.getId()));
                if (alreadyMatched) {
                    continue;
                }
                double titleSim = calculateTitleSimilarity(title, mat.getTitle());
                double sizeSim = calculateFileSizeSimilarity(fileSize, mat.getFileSize());
                if (titleSim >= TITLE_SIMILARITY_THRESHOLD && sizeSim >= 0.95) {
                    double similarity = (titleSim * 0.6) + (sizeSim * 0.4);
                    String reason = String.format("标题相似度 %.0f%%，文件大小接近 (±%.0fKB)",
                            titleSim * 100, Math.abs(fileSize - mat.getFileSize()) / 1024.0);
                    matchedMaterials.add(new DuplicateCheckResult.MatchedMaterial(mat, similarity, reason));
                }
            }
        }

        if (title != null && !title.trim().isEmpty()) {
            List<Material> titleMatches = materialRepository.findByTitleContainingAndStatus(
                    extractKeyword(title), 1);
            for (Material mat : titleMatches) {
                if (excludeUserId != null && mat.getUserId().equals(excludeUserId)) {
                    continue;
                }
                boolean alreadyMatched = matchedMaterials.stream()
                        .anyMatch(m -> m.getId().equals(mat.getId()));
                if (alreadyMatched) {
                    continue;
                }
                double titleSim = calculateTitleSimilarity(title, mat.getTitle());
                if (titleSim >= TITLE_SIMILARITY_THRESHOLD) {
                    double sizeSim = calculateFileSizeSimilarity(fileSize, mat.getFileSize());
                    double similarity = (titleSim * 0.7) + (sizeSim * 0.3);
                    if (similarity >= COMBINED_SCORE_THRESHOLD) {
                        String reason = String.format("标题相似度 %.0f%%，疑似同版资料", titleSim * 100);
                        matchedMaterials.add(new DuplicateCheckResult.MatchedMaterial(mat, similarity, reason));
                    }
                }
            }
        }

        matchedMaterials.sort(Comparator.comparingDouble(DuplicateCheckResult.MatchedMaterial::getSimilarity).reversed());

        if (matchedMaterials.size() > 5) {
            matchedMaterials = matchedMaterials.subList(0, 5);
        }

        boolean isDuplicate = !matchedMaterials.isEmpty();
        double maxSimilarity = matchedMaterials.stream()
                .mapToDouble(DuplicateCheckResult.MatchedMaterial::getSimilarity)
                .max()
                .orElse(0.0);

        result.setDuplicate(isDuplicate);
        result.setSimilarityScore(maxSimilarity);
        result.setMatchedMaterials(matchedMaterials);

        if (isDuplicate) {
            if (maxSimilarity >= HASH_MATCH_THRESHOLD) {
                result.setMatchType("exact");
                result.setWarningMessage("检测到库内存在内容完全一致的资料，请确认是否重复上传。");
            } else if (maxSimilarity >= 0.9) {
                result.setMatchType("high");
                result.setWarningMessage("检测到库内存在高度相似的资料，建议先查看是否已有同版讲义或试卷。");
            } else {
                result.setMatchType("partial");
                result.setWarningMessage("检测到库内存在部分相似的资料，建议确认后再上传。");
            }
        }

        return result;
    }

    public void saveMaterialHash(Material material, MultipartFile file) throws IOException {
        String hash = fileUtil.calculateFileHash(file);
        material.setFileHash(hash);
    }

    public void saveMaterialHash(Material material, java.io.File file) throws IOException {
        String hash = fileUtil.calculateFileHash(file);
        material.setFileHash(hash);
    }

    private double calculateCombinedScore(Material material, String title, Long fileSize, String fileHash) {
        double hashScore = 0.0;
        if (fileHash != null && !fileHash.isEmpty() && material.getFileHash() != null && !material.getFileHash().isEmpty()) {
            hashScore = fileHash.equals(material.getFileHash()) ? 1.0 : 0.0;
        }

        double titleScore = calculateTitleSimilarity(title, material.getTitle());
        double sizeScore = calculateFileSizeSimilarity(fileSize, material.getFileSize());

        if (hashScore >= HASH_MATCH_THRESHOLD) {
            return hashScore;
        }

        return (titleScore * 0.5) + (sizeScore * 0.3) + (hashScore * 0.2);
    }

    private double calculateTitleSimilarity(String title1, String title2) {
        if (title1 == null || title2 == null || title1.isEmpty() || title2.isEmpty()) {
            return 0.0;
        }

        String t1 = normalizeTitle(title1);
        String t2 = normalizeTitle(title2);

        if (t1.equals(t2)) {
            return 1.0;
        }

        if (t1.contains(t2) || t2.contains(t1)) {
            return 0.9;
        }

        int distance = levenshteinDistance(t1, t2);
        int maxLength = Math.max(t1.length(), t2.length());
        if (maxLength == 0) {
            return 0.0;
        }

        return 1.0 - (double) distance / maxLength;
    }

    private double calculateFileSizeSimilarity(Long size1, Long size2) {
        if (size1 == null || size2 == null || size1 <= 0 || size2 <= 0) {
            return 0.0;
        }

        long diff = Math.abs(size1 - size2);
        long max = Math.max(size1, size2);

        if (diff == 0) {
            return 1.0;
        }

        if (diff <= 1024) {
            return 0.99;
        }

        if (diff <= 10 * 1024) {
            return 0.95;
        }

        return 1.0 - (double) diff / max;
    }

    private String normalizeTitle(String title) {
        if (title == null) {
            return "";
        }
        return title.toLowerCase()
                .replaceAll("[\\s\\p{Punct}]", "")
                .replaceAll("(答案|解析|版|上册|下册|必修|选修|期末|期中|月考|周测|训练|练习|试卷|课件|教案|讲义|知识点|总结|汇总|复习|专题)", "");
    }

    private String extractKeyword(String title) {
        if (title == null || title.length() < 2) {
            return title;
        }
        String normalized = normalizeTitle(title);
        if (normalized.length() <= 4) {
            return normalized;
        }
        return normalized.substring(0, Math.min(8, normalized.length()));
    }

    private int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }

        return dp[len1][len2];
    }
}
