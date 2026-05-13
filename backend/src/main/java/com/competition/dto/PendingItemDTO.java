package com.competition.dto;

import java.time.LocalDateTime;

public class PendingItemDTO {
    private Long id;
    private String type;           // COMPETITION/PROJECT/PAPER/SOFTWARE
    private String title;          // unified name/title
    private String applicantName;
    private Long applicantId;
    private LocalDateTime applyTime;
    private String status;         // PENDING/PENDING_LEADER/REJECTED/ARCHIVED
    private String extraInfo;      // category for comp, level for project/paper

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    public LocalDateTime getApplyTime() { return applyTime; }
    public void setApplyTime(LocalDateTime applyTime) { this.applyTime = applyTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getExtraInfo() { return extraInfo; }
    public void setExtraInfo(String extraInfo) { this.extraInfo = extraInfo; }
}
