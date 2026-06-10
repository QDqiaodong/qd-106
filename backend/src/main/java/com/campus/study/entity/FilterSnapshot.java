package com.campus.study.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "filter_snapshot")
public class FilterSnapshot extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "keyword", length = 255)
    private String keyword;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "grade_id")
    private Long gradeId;

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "sort")
    private Integer sort;
}
