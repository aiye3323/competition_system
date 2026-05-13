package com.competition.service;

import com.competition.dto.AuditLogDTO;
import com.competition.dto.ReviewRequest;
import com.competition.entity.AuditLog;
import com.competition.entity.Competition;
import com.competition.entity.User;
import com.competition.repository.AuditLogRepository;
import com.competition.repository.CompetitionRepository;
import com.competition.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final CompetitionRepository competitionRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public AuditLogDTO secretaryReview(Long competitionId, Long auditorId, ReviewRequest request) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("竞赛记录不存在"));

        if (!"PENDING".equals(competition.getStatus())) {
            throw new RuntimeException("当前状态不可进行一审");
        }

        User auditor = userRepository.findById(auditorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"SECRETARY".equals(auditor.getRole())) {
            throw new RuntimeException("仅科研秘书可进行一审");
        }

        AuditLog log = new AuditLog();
        log.setAchievementType("COMPETITION");
        log.setAchievementId(competitionId);
        log.setAuditorId(auditorId);
        log.setAuditorRole("SECRETARY");
        log.setResult(request.getResult());
        log.setOpinion(request.getOpinion());
        log.setAuditTime(LocalDateTime.now());
        auditLogRepository.save(log);

        if ("APPROVED".equals(request.getResult())) {
            competition.setStatus("FIRST_REVIEW");
        } else {
            competition.setStatus("REJECTED");
        }
        competitionRepository.save(competition);

        return toDTO(log);
    }

    @Transactional
    public AuditLogDTO leaderReview(Long competitionId, Long auditorId, ReviewRequest request) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("竞赛记录不存在"));

        if (!"FIRST_REVIEW".equals(competition.getStatus())) {
            throw new RuntimeException("当前状态不可进行终审");
        }

        User auditor = userRepository.findById(auditorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"LEADER".equals(auditor.getRole())) {
            throw new RuntimeException("仅学院领导可进行终审");
        }

        AuditLog log = new AuditLog();
        log.setAchievementType("COMPETITION");
        log.setAchievementId(competitionId);
        log.setAuditorId(auditorId);
        log.setAuditorRole("LEADER");
        log.setResult(request.getResult());
        log.setOpinion(request.getOpinion());
        log.setAuditTime(LocalDateTime.now());
        auditLogRepository.save(log);

        if ("APPROVED".equals(request.getResult())) {
            competition.setStatus("APPROVED");
        } else {
            competition.setStatus("REJECTED");
        }
        competitionRepository.save(competition);

        return toDTO(log);
    }

    public List<AuditLogDTO> getAuditLogs(String achievementType, Long achievementId) {
        List<AuditLog> logs = auditLogRepository
                .findByAchievementTypeAndAchievementIdOrderByAuditTimeAsc(
                        achievementType, achievementId);
        return logs.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private AuditLogDTO toDTO(AuditLog log) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setId(log.getId());
        dto.setAchievementType(log.getAchievementType());
        dto.setAchievementId(log.getAchievementId());
        dto.setAuditorId(log.getAuditorId());
        dto.setAuditorRole(log.getAuditorRole());
        dto.setResult(log.getResult());
        dto.setOpinion(log.getOpinion());
        dto.setAuditTime(log.getAuditTime());

        userRepository.findById(log.getAuditorId()).ifPresent(u ->
                dto.setAuditorName(u.getRealName()));

        return dto;
    }
}
