package com.campus.study.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "correction", indexes = {
    @Index(name = "idx_material", columnList = "material_id"),
    @Index(name = "idx_user", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_handler", columnList = "handler_id")
})
public class Correction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "error_description", columnDefinition = "TEXT", nullable = false)
    private String errorDescription;

    @Column(name = "correction_suggestion", columnDefinition = "TEXT")
    private String correctionSuggestion;

    @Column(name = "status")
    private Integer status = 0;

    @Column(name = "handler_id")
    private Long handlerId;

    @Column(name = "handle_remark", length = 500)
    private String handleRemark = "";

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private String materialTitle;

    @Transient
    private String submitterNickname;
}
