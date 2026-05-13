package com.competition.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SoftwareDTO {
    private Long id;
    private String softwareName;
    private String affiliation;
    private String copyrightOwner;
    private String registrationNumber;
    private String materialPath;
    private LocalDate registrationDate;
    private String certificatePath;
    private String status;
    private Long applicantId;
    private String applicantName;
    private LocalDateTime applyTime;
    private LocalDateTime archiveTime;
    private List<FileInfo> fileUrlList;
}
