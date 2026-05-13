package com.competition.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class SoftwareSubmitRequest {
    private String softwareName;
    private String affiliation;
    private String copyrightOwner;
    private String registrationNumber;
    private LocalDate registrationDate;
    private List<Long> fileIds;
}
