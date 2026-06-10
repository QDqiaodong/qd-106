package com.campus.study.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "material")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover", length = 255)
    private String cover = "";

    @Column(name = "category_id")
    private Long categoryId = 0L;

    @Column(name = "grade_id")
    private Long gradeId = 0L;

    @Column(name = "subject_id")
    private Long subjectId = 0L;

    @Column(name = "file_url", length = 255)
    private String fileUrl = "";

    @Column(name = "file_size")
    private Long fileSize = 0L;

    @Column(name = "total_pages")
    private Integer totalPages = 0;

    @Column(name = "download_count")
    private Integer downloadCount = 0;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "user_id")
    private Long userId = 0L;

    @Column(name = "status")
    private Integer status = 1;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private Boolean favorited = false;

    @Transient
    private Object readingProgress;
}
