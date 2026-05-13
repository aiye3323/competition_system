package com.competition.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PaperDTO {
    private Long id;
    private String title;
    private LocalDate submissionDate;
    private LocalDate acceptanceDate;
    private String journalName;
    private String keywords;
    private String journalLevel;
    private String authors;
    private String status;
    private Long applicantId;
    private String applicantName;
    private LocalDateTime applyTime;
    private LocalDateTime archiveTime;
    private List<FileInfo> fileUrlList;
}
