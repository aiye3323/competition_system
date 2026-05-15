package com.competition.controller;

import com.competition.dto.DashboardStats;
import com.competition.dto.Result;
import com.competition.service.ExportService;
import com.competition.service.StatisticsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final ExportService exportService;

    public StatisticsController(StatisticsService statisticsService, ExportService exportService) {
        this.statisticsService = statisticsService;
        this.exportService = exportService;
    }

    @GetMapping("/dashboard")
    public Result<DashboardStats> dashboard() {
        return Result.success(statisticsService.getDashboardStats());
    }

    @GetMapping("/export-all")
    public ResponseEntity<?> exportAll(Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).body(Result.error(401, "请先登录"));

        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst().map(Object::toString)
                .map(a -> a.replace("ROLE_", ""))
                .orElse("STUDENT");

        byte[] data = exportService.exportAllAchievements(userId, role);

        String filename = "科研成果汇总_" + java.time.LocalDate.now() + ".xlsx";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded)
                .body(data);
    }
}
