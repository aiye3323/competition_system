package com.competition.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProjectDTO {
    private Long id;
    private String projectName;
    private String projectLevel;
    private String projectType;
    private String advisor;
    private String members;
    private LocalDate establishTime;
    private String proposalPath;
    private String conclusionPath;
    private String certificatePath;
    private String status;
    private Long applicantId;
    private String applicantName;
    private LocalDateTime applyTime;
    private LocalDateTime archiveTime;
    private List<FileInfo> fileUrlList;
}
