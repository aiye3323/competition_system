package com.competition.repository;

import com.competition.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreateTimeDesc(Long userId);

    Integer countByUserIdAndIsRead(Long userId, Integer isRead);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);
}
