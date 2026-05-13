package com.competition.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_file")
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "f_id")
    private Long id;

    @Column(name = "f_original_name", nullable = false, length = 500)
    private String originalName;

    @Column(name = "f_storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "f_file_size", nullable = false)
    private Long fileSize;

    @Column(name = "f_file_type", nullable = false, length = 20)
    private String fileType;

    @Column(name = "f_related_type", length = 20)
    private String relatedType;

    @Column(name = "f_related_id")
    private Long relatedId;

    @Column(name = "f_uploader_id", nullable = false)
    private Long uploaderId;

    @Column(name = "f_upload_time")
    private LocalDateTime uploadTime;

    @Column(name = "f_create_time", updatable = false)
    private LocalDateTime createTime;

    @Column(name = "f_update_time")
    private LocalDateTime updateTime;

    @Column(name = "f_is_deleted")
    private Integer isDeleted = 0;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (uploadTime == null) {
            uploadTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
