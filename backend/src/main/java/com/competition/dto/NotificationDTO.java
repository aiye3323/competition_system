package com.competition.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationDTO {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private Long competitionId;
    private Integer isRead;
    private LocalDateTime createTime;
}
