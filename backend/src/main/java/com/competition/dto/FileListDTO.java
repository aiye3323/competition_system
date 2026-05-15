package com.competition.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FileListDTO {
    private Long id;
    private String originalName;
    private String storagePath;
    private Long fileSize;
    private String fileType;
    private String materialType;
    private String relatedType;
    private Long relatedId;
    private Long uploaderId;
    private String uploaderName;
    private LocalDateTime uploadTime;
}
