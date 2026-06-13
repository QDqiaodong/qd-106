package com.campus.study.vo;

import com.campus.study.entity.Material;

import java.util.List;

public class DuplicateCheckResult {

    private boolean duplicate;

    private double similarityScore;

    private String matchType;

    private List<MatchedMaterial> matchedMaterials;

    private String warningMessage;

    public static class MatchedMaterial {
        private Long id;
        private String title;
        private String fileUrl;
        private Long fileSize;
        private double similarity;
        private String matchReason;
        private String createdAt;

        public MatchedMaterial() {
        }

        public MatchedMaterial(Material material, double similarity, String matchReason) {
            this.id = material.getId();
            this.title = material.getTitle();
            this.fileUrl = material.getFileUrl();
            this.fileSize = material.getFileSize();
            this.similarity = similarity;
            this.matchReason = matchReason;
            this.createdAt = material.getCreatedAt() != null ? material.getCreatedAt().toString() : "";
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getFileUrl() {
            return fileUrl;
        }

        public void setFileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }

        public double getSimilarity() {
            return similarity;
        }

        public void setSimilarity(double similarity) {
            this.similarity = similarity;
        }

        public String getMatchReason() {
            return matchReason;
        }

        public void setMatchReason(String matchReason) {
            this.matchReason = matchReason;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public List<MatchedMaterial> getMatchedMaterials() {
        return matchedMaterials;
    }

    public void setMatchedMaterials(List<MatchedMaterial> matchedMaterials) {
        this.matchedMaterials = matchedMaterials;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }
}
