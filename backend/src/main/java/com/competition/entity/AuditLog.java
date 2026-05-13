package com.competition.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "f_id")
    private Long id;

    @Column(name = "f_achievement_type", nullable = false, length = 20)
    private String achievementType;

    @Column(name = "f_achievement_id", nullable = false)
    private Long achievementId;

    @Column(name = "f_auditor_id", nullable = false)
    private Long auditorId;

    @Column(name = "f_auditor_role", nullable = false, length = 20)
    private String auditorRole;

    @Column(name = "f_result", length = 20)
    private String result;

    @Column(name = "f_opinion", columnDefinition = "TEXT")
    private String opinion;

    @Column(name = "f_audit_time")
    private LocalDateTime auditTime;

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
        if (auditTime == null) {
            auditTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
