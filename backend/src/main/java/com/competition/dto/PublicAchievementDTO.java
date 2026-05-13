package com.competition.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PublicAchievementDTO {
    private String type;
    private String typeLabel;
    private Long id;
    private String title;
    private String applicantName;
    private String applicantCollege;
    private String level1;
    private String level2;
    private LocalDate achievementDate;
    private String status;
}
