package com.competition.dto;

import lombok.Data;

@Data
public class OcrResultDTO {
    private boolean success;
    private String errorMessage;
    private String rawText;
    private String category;

    // 通用
    private String name;
    private String submitter;
    private String submitTime;

    // 竞赛
    private String awardLevel;
    private String awardRank;
    private String hostUnit;
    private String competitionDate;

    // 项目
    private String projectLevel;
    private String projectType;
    private String advisor;
    private String establishTime;

    // 软著
    private String softwareName;
    private String copyrightHolder;
    private String registrationNo;
    private String registrationDate;

    // 论文
    private String paperTitle;
    private String journalName;
    private String journalLevel;
    private String acceptanceDate;

    public static OcrResultDTO fail(String msg) {
        OcrResultDTO dto = new OcrResultDTO();
        dto.success = false;
        dto.errorMessage = msg;
        return dto;
    }

    public static OcrResultDTO ok(String rawText) {
        OcrResultDTO dto = new OcrResultDTO();
        dto.success = true;
        dto.rawText = rawText;
        return dto;
    }
}
