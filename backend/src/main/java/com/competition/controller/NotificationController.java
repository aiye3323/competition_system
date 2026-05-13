package com.competition.controller;

import com.competition.dto.NotificationDTO;
import com.competition.dto.Result;
import com.competition.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Result<List<NotificationDTO>> getNotifications(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<NotificationDTO> list = notificationService.getNotifications(userId);
        return Result.success(list);
    }

    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        notificationService.markAsRead(id, userId);
        return Result.success(null);
    }

    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        int count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }
}
