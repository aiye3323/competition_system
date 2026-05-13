package com.competition.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "f_id")
    private Long id;

    @Column(name = "f_project_name", nullable = false, length = 200)
    private String projectName;

    @Column(name = "f_project_level", nullable = false, length = 20)
    private String projectLevel;

    @Column(name = "f_project_type", nullable = false, length = 30)
    private String projectType;

    @Column(name = "f_advisor", length = 100)
    private String advisor;

    @Column(name = "f_members", columnDefinition = "TEXT")
    private String members;

    @Column(name = "f_establish_time")
    private LocalDate establishTime;

    @Column(name = "f_proposal_path", length = 500)
    private String proposalPath;

    @Column(name = "f_conclusion_path", length = 500)
    private String conclusionPath;

    @Column(name = "f_certificate_path", length = 500)
    private String certificatePath;

    @Column(name = "f_status", nullable = false, length = 20)
    private String status = "PENDING";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "f_applicant_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User applicant;

    @Column(name = "f_apply_time")
    private LocalDateTime applyTime;

    @Column(name = "f_archive_time")
    private LocalDateTime archiveTime;

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
        if (applyTime == null) {
            applyTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
