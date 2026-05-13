package com.competition.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_competition")
public class Competition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "f_id")
    private Long id;

    @Column(name = "f_category", nullable = false, length = 10)
    private String category;

    @Column(name = "f_competition_name", nullable = false, length = 200)
    private String competitionName;

    @Column(name = "f_award_level", length = 20)
    private String awardLevel;

    @Column(name = "f_award_grade", length = 20)
    private String awardGrade;

    @Column(name = "f_award_unit", length = 200)
    private String awardUnit;

    @Column(name = "f_organizer", length = 200)
    private String organizer;

    @Column(name = "f_co_organizer", length = 200)
    private String coOrganizer;

    @Column(name = "f_award_date")
    private LocalDate awardDate;

    @Column(name = "f_advisor", length = 100)
    private String advisor;

    @Column(name = "f_participants", columnDefinition = "TEXT")
    private String participants;

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
