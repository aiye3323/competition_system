package com.competition.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AuditDetailDTO {
    private Long id;
    private String type;              // COMPETITION/PROJECT/PAPER/SOFTWARE
    private String title;
    private String applicantName;
    private Long applicantId;
    private LocalDateTime applyTime;
    private String status;
    private Map<String, Object> fields;  // type-specific fields
    private List<FileInfo> files;
    private List<AuditLogDTO> auditLogs;

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
    public Map<String, Object> getFields() { return fields; }
    public void setFields(Map<String, Object> fields) { this.fields = fields; }
    public List<FileInfo> getFiles() { return files; }
    public void setFiles(List<FileInfo> files) { this.files = files; }
    public List<AuditLogDTO> getAuditLogs() { return auditLogs; }
    public void setAuditLogs(List<AuditLogDTO> auditLogs) { this.auditLogs = auditLogs; }

    public static class FileInfo {
        private Long id;
        private String originalName;
        private String fileType;    // IMAGE/DOCUMENT/ARCHIVE/VIDEO/OTHER
        private Long fileSize;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getOriginalName() { return originalName; }
        public void setOriginalName(String originalName) { this.originalName = originalName; }
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    }
}
