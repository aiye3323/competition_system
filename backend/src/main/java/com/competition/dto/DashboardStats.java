package com.competition.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DashboardStats {
    private long totalCompetitions;
    private long totalProjects;
    private long totalPapers;
    private long totalSoftware;
    private long totalAchievements;
    private List<TypeStat> byType;
    private Map<String, Long> byCollege;
    private List<YearStat> byYear;
    private Map<String, Long> byAwardLevel;
    private Map<String, Long> byProjectLevel;
    private Map<String, Long> byJournalLevel;
    private List<RecentAchievement> recentItems;

    @Data
    public static class TypeStat {
        private String type;
        private String label;
        private long count;
        public TypeStat(String type, String label, long count) {
            this.type = type;
            this.label = label;
            this.count = count;
        }
    }

    @Data
    public static class YearStat {
        private int year;
        private long competitions;
        private long projects;
        private long papers;
        private long software;
        private long total;
    }

    @Data
    public static class RecentAchievement {
        private String type;
        private String typeLabel;
        private Long id;
        private String title;
        private String applicantName;
        private String level;
        private String date;
    }
}
