package com.competition.controller;

import com.competition.dto.AuditLogDTO;
import com.competition.dto.Result;
import com.competition.dto.ReviewRequest;
import com.competition.service.ReviewService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/competitions")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/{id}/secretary-review")
    public Result<AuditLogDTO> secretaryReview(
            @PathVariable Long id,
            @RequestBody ReviewRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AuditLogDTO result = reviewService.secretaryReview(id, userId, request);
        return Result.success(result);
    }

    @PostMapping("/{id}/leader-review")
    public Result<AuditLogDTO> leaderReview(
            @PathVariable Long id,
            @RequestBody ReviewRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AuditLogDTO result = reviewService.leaderReview(id, userId, request);
        return Result.success(result);
    }

    @GetMapping("/{id}/audit-logs")
    public Result<List<AuditLogDTO>> getAuditLogs(@PathVariable Long id) {
        List<AuditLogDTO> logs = reviewService.getAuditLogs("COMPETITION", id);
        return Result.success(logs);
    }
}
