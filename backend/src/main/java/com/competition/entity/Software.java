package com.competition.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_software")
public class Software {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "f_id")
    private Long id;

    @Column(name = "f_software_name", nullable = false, length = 200)
    private String softwareName;

    @Column(name = "f_affiliation", length = 100)
    private String affiliation;

    @Column(name = "f_copyright_owner", length = 200)
    private String copyrightOwner;

    @Column(name = "f_registration_number", length = 100)
    private String registrationNumber;

    @Column(name = "f_material_path", length = 500)
    private String materialPath;

    @Column(name = "f_registration_date")
    private LocalDate registrationDate;

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
