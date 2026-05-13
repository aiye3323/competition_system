package com.competition.repository;

import com.competition.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByAchievementTypeAndAchievementIdOrderByAuditTimeAsc(
            String achievementType, Long achievementId);
}
