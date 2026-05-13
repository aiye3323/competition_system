package com.competition.controller;

import com.competition.dto.DashboardStats;
import com.competition.dto.Result;
import com.competition.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/dashboard")
    public Result<DashboardStats> dashboard() {
        return Result.success(statisticsService.getDashboardStats());
    }
}
