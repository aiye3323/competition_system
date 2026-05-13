package com.competition.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditLogDTO {
    private Long id;
    private String achievementType;
    private Long achievementId;
    private Long auditorId;
    private String auditorName;
    private String auditorRole;
    private String result;
    private String opinion;
    private LocalDateTime auditTime;
}
