package com.campus.study.vo;

import java.io.Serializable;
import java.util.List;

public class MaterialThumbnailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    private String description;

    private Long categoryId;

    private String categoryName;

    private Long gradeId;

    private String gradeName;

    private Long subjectId;

    private String subjectName;

    private Integer totalPages;

    private List<ThumbnailItem> thumbnails;

    public static class ThumbnailItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private Integer pageNumber;

        private String imageUrl;

        private String label;

        public ThumbnailItem() {
        }

        public ThumbnailItem(Integer pageNumber, String imageUrl, String label) {
            this.pageNumber = pageNumber;
            this.imageUrl = imageUrl;
            this.label = label;
        }

        public Integer getPageNumber() {
            return pageNumber;
        }

        public void setPageNumber(Integer pageNumber) {
            this.pageNumber = pageNumber;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getGradeId() {
        return gradeId;
    }

    public void setGradeId(Long gradeId) {
        this.gradeId = gradeId;
    }

    public String getGradeName() {
        return gradeName;
    }

    public void setGradeName(String gradeName) {
        this.gradeName = gradeName;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public List<ThumbnailItem> getThumbnails() {
        return thumbnails;
    }

    public void setThumbnails(List<ThumbnailItem> thumbnails) {
        this.thumbnails = thumbnails;
    }
}
