package com.competition.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CompetitionDTO {
    private Long id;
    private String category;
    private String competitionName;
    private String awardLevel;
    private String awardGrade;
    private String awardUnit;
    private String organizer;
    private String coOrganizer;
    private LocalDate awardDate;
    private String advisor;
    private String participants;
    private String certificatePath;
    private String status;
    private Long applicantId;
    private String applicantName;
    private LocalDateTime applyTime;
    private LocalDateTime archiveTime;
    private List<FileInfo> fileUrlList;
}
