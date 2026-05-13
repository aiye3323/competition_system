package com.competition.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class ProjectSubmitRequest {
    private String projectName;
    private String projectLevel;
    private String projectType;
    private String advisor;
    private String members;
    private LocalDate establishTime;
    private List<Long> fileIds;
}
