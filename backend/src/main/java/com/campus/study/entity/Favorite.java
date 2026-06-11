package com.campus.study.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "favorite", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "material_id"})
})
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "review_status")
    private Integer reviewStatus = 0;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
