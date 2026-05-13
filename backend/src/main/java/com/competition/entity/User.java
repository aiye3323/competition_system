package com.competition.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "f_id")
    private Long id;

    @Column(name = "f_username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "f_password", nullable = false, length = 255)
    private String password;

    @Column(name = "f_real_name", length = 50)
    private String realName;

    @Column(name = "f_role", nullable = false, length = 20)
    private String role = "STUDENT";

    @Column(name = "f_email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "f_student_id", length = 50)
    private String studentId;

    @Column(name = "f_college", length = 100)
    private String college;

    @Column(name = "f_register_time")
    private LocalDateTime registerTime;

    @Column(name = "f_last_login_time")
    private LocalDateTime lastLoginTime;

    @Column(name = "f_status")
    private Integer status = 1;

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
        if (registerTime == null) {
            registerTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
