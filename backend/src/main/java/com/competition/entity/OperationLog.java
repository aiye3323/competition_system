package com.competition.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_operation_log")
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "f_id")
    private Long id;

    @Column(name = "f_user_id", nullable = false)
    private Long userId;

    @Column(name = "f_username", length = 50)
    private String username;

    @Column(name = "f_operation_type", nullable = false, length = 30)
    private String operationType;

    @Column(name = "f_target_type", length = 30)
    private String targetType;

    @Column(name = "f_target_id")
    private Long targetId;

    @Column(name = "f_description", length = 500)
    private String description;

    @Column(name = "f_ip", length = 50)
    private String ip;

    @Column(name = "f_operate_time", nullable = false)
    private LocalDateTime operateTime;

    @Column(name = "f_create_time", updatable = false)
    private LocalDateTime createTime;

    @Column(name = "f_is_deleted")
    private Integer isDeleted = 0;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
