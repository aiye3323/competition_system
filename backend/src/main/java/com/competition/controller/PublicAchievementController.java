package com.competition.controller;

import com.competition.dto.PublicAchievementDTO;
import com.competition.dto.Result;
import com.competition.service.ExportService;
import com.competition.service.StatisticsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/achievements")
public class PublicAchievementController {

    private final StatisticsService statisticsService;
    private final ExportService exportService;

    public PublicAchievementController(StatisticsService statisticsService, ExportService exportService) {
        this.statisticsService = statisticsService;
        this.exportService = exportService;
    }

    @GetMapping("/public")
    public Result<Map<String, Object>> publicList(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String level1,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String year,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<PublicAchievementDTO> list = statisticsService.getPublicList(type, level1, keyword, year, page, size);
        long total = statisticsService.countPublic(type, level1, keyword, year);
        Map<String, Object> data = new HashMap<>();
        data.put("content", list);
        data.put("totalElements", total);
        return Result.success(data);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String level1,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String year) {
        byte[] bytes = exportService.exportPublicAchievements(type, level1, keyword, year);
        String filename = URLEncoder.encode("全院成果汇总.xlsx", StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }
}
