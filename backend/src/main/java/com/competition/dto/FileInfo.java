package com.competition.dto;

import lombok.Data;

@Data
public class FileInfo {
    private Long id;
    private String url;
    private String originalName;
    private String fileType;
}
