package com.competition.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class PaperSubmitRequest {
    private String title;
    private LocalDate submissionDate;
    private LocalDate acceptanceDate;
    private String journalName;
    private String keywords;
    private String journalLevel;
    private String authors;
    private List<Long> fileIds;
}
