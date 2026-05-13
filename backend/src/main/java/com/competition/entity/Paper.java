package com.competition.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_paper")
public class Paper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "f_id")
    private Long id;

    @Column(name = "f_title", nullable = false, length = 300)
    private String title;

    @Column(name = "f_submission_date")
    private LocalDate submissionDate;

    @Column(name = "f_acceptance_date")
    private LocalDate acceptanceDate;

    @Column(name = "f_journal_name", length = 200)
    private String journalName;

    @Column(name = "f_keywords", length = 500)
    private String keywords;

    @Column(name = "f_journal_level", length = 30)
    private String journalLevel;

    @Column(name = "f_authors", columnDefinition = "TEXT")
    private String authors;

    @Column(name = "f_draft_path", length = 500)
    private String draftPath;

    @Column(name = "f_final_path", length = 500)
    private String finalPath;

    @Column(name = "f_review_comments_path", length = 500)
    private String reviewCommentsPath;

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
