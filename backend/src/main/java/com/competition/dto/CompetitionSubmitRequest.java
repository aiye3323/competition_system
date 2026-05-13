package com.competition.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class CompetitionSubmitRequest {
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
    private List<Long> fileIds;
}
