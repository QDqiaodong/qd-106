package com.campus.study.entity;

import com.campus.study.enums.FileInspectionExceptionType;
import com.campus.study.enums.FileInspectionHandleStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "file_inspection_record")
public class FileInspectionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "material_title", length = 255)
    private String materialTitle;

    @Column(name = "file_url", length = 255)
    private String fileUrl;

    @Column(name = "exception_type", nullable = false)
    private Integer exceptionType;

    @Column(name = "exception_detail", columnDefinition = "TEXT")
    private String exceptionDetail;

    @Column(name = "expected_file_size")
    private Long expectedFileSize;

    @Column(name = "actual_file_size")
    private Long actualFileSize;

    @Column(name = "expected_file_hash", length = 64)
    private String expectedFileHash;

    @Column(name = "actual_file_hash", length = 64)
    private String actualFileHash;

    @Column(name = "handle_status", nullable = false)
    private Integer handleStatus = 0;

    @Column(name = "handler_id")
    private Long handlerId;

    @Column(name = "handle_remark", length = 500)
    private String handleRemark;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(name = "inspection_batch", length = 64)
    private String inspectionBatch;

    @CreationTimestamp
    @Column(name = "discovered_at", updatable = false)
    private LocalDateTime discoveredAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private String exceptionTypeName;

    @Transient
    private String handleStatusName;

    @Transient
    private Material material;

    @PostLoad
    public void fillTransientFields() {
        FileInspectionExceptionType exType = FileInspectionExceptionType.fromCode(this.exceptionType);
        if (exType != null) {
            this.exceptionTypeName = exType.getName();
        }
        FileInspectionHandleStatus hStatus = FileInspectionHandleStatus.fromCode(this.handleStatus);
        if (hStatus != null) {
            this.handleStatusName = hStatus.getName();
        }
    }
}
