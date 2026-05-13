package com.competition.service;

import com.competition.dto.NotificationDTO;
import com.competition.entity.Notification;
import com.competition.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void createNotification(Long userId, String title, String content, Long competitionId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setCompetitionId(competitionId);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreateTimeDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new RuntimeException("通知不存在"));
        notification.setIsRead(1);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public int getUnreadCount(Long userId) {
        Integer count = notificationRepository.countByUserIdAndIsRead(userId, 0);
        return count != null ? count : 0;
    }

    private NotificationDTO toDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUserId());
        dto.setTitle(notification.getTitle());
        dto.setContent(notification.getContent());
        dto.setCompetitionId(notification.getCompetitionId());
        dto.setIsRead(notification.getIsRead());
        dto.setCreateTime(notification.getCreateTime());
        return dto;
    }
}
