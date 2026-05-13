package com.competition.service;

import com.competition.dto.DashboardStats;
import com.competition.dto.PublicAchievementDTO;
import com.competition.entity.Competition;
import com.competition.entity.Paper;
import com.competition.entity.Project;
import com.competition.entity.Software;
import com.competition.repository.CompetitionRepository;
import com.competition.repository.PaperRepository;
import com.competition.repository.ProjectRepository;
import com.competition.repository.SoftwareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final CompetitionRepository competitionRepository;
    private final ProjectRepository projectRepository;
    private final PaperRepository paperRepository;
    private final SoftwareRepository softwareRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DashboardStats getDashboardStats() {
        List<Competition> competitions = competitionRepository.findByStatusAndIsDeleted("ARCHIVED", 0);
        List<Project> projects = projectRepository.findByStatusAndIsDeleted("ARCHIVED", 0);
        List<Paper> papers = paperRepository.findByStatusAndIsDeleted("ARCHIVED", 0);
        List<Software> softwares = softwareRepository.findByStatusAndIsDeleted("ARCHIVED", 0);

        DashboardStats stats = new DashboardStats();
        stats.setTotalCompetitions(competitions.size());
        stats.setTotalProjects(projects.size());
        stats.setTotalPapers(papers.size());
        stats.setTotalSoftware(softwares.size());
        stats.setTotalAchievements(competitions.size() + projects.size() + papers.size() + softwares.size());

        stats.setByType(Arrays.asList(
                new DashboardStats.TypeStat("COMPETITION", "学科竞赛", competitions.size()),
                new DashboardStats.TypeStat("PROJECT", "创新项目", projects.size()),
                new DashboardStats.TypeStat("PAPER", "学术论文", papers.size()),
                new DashboardStats.TypeStat("SOFTWARE", "软件著作", softwares.size())
        ));

        // 按年度统计
        Map<Integer, DashboardStats.YearStat> yearMap = new TreeMap<>();
        addToYearMap(yearMap, competitions, c -> getYear(c.getAwardDate(), c.getApplyTime()), "competitions");
        addToYearMap(yearMap, projects, p -> getYear(p.getEstablishTime(), p.getApplyTime()), "projects");
        addToYearMap(yearMap, papers, p -> getYear(p.getAcceptanceDate(), p.getApplyTime()), "papers");
        addToYearMap(yearMap, softwares, s -> getYear(s.getRegistrationDate(), s.getApplyTime()), "software");
        for (DashboardStats.YearStat ys : yearMap.values()) {
            ys.setTotal(ys.getCompetitions() + ys.getProjects() + ys.getPapers() + ys.getSoftware());
        }
        stats.setByYear(new ArrayList<>(yearMap.values()));

        stats.setByAwardLevel(competitions.stream()
                .filter(c -> c.getAwardLevel() != null)
                .collect(Collectors.groupingBy(Competition::getAwardLevel, Collectors.counting())));
        stats.setByProjectLevel(projects.stream()
                .filter(p -> p.getProjectLevel() != null)
                .collect(Collectors.groupingBy(Project::getProjectLevel, Collectors.counting())));
        stats.setByJournalLevel(papers.stream()
                .filter(p -> p.getJournalLevel() != null)
                .collect(Collectors.groupingBy(Paper::getJournalLevel, Collectors.counting())));

        // 按学院统计
        Map<String, Long> collegeMap = new HashMap<>();
        addCollege(collegeMap, competitions, c -> c.getApplicant() != null ? c.getApplicant().getCollege() : "未知");
        addCollege(collegeMap, projects, p -> p.getApplicant() != null ? p.getApplicant().getCollege() : "未知");
        addCollege(collegeMap, papers, p -> p.getApplicant() != null ? p.getApplicant().getCollege() : "未知");
        addCollege(collegeMap, softwares, s -> s.getApplicant() != null ? s.getApplicant().getCollege() : "未知");
        stats.setByCollege(collegeMap);

        stats.setRecentItems(buildRecentItems(competitions, projects, papers, softwares));

        return stats;
    }

    private <T> void addToYearMap(Map<Integer, DashboardStats.YearStat> map, List<T> items,
                                   java.util.function.Function<T, Integer> yearFn, String field) {
        for (T item : items) {
            int year = yearFn.apply(item);
            map.computeIfAbsent(year, k -> { DashboardStats.YearStat ys = new DashboardStats.YearStat(); ys.setYear(k); return ys; });
            DashboardStats.YearStat ys = map.get(year);
            switch (field) {
                case "competitions" -> ys.setCompetitions(ys.getCompetitions() + 1);
                case "projects" -> ys.setProjects(ys.getProjects() + 1);
                case "papers" -> ys.setPapers(ys.getPapers() + 1);
                case "software" -> ys.setSoftware(ys.getSoftware() + 1);
            }
        }
    }

    private <T> void addCollege(Map<String, Long> map, List<T> items, java.util.function.Function<T, String> collegeFn) {
        for (T item : items) {
            String college = collegeFn.apply(item);
            if (college == null || college.isEmpty()) college = "未知";
            map.merge(college, 1L, Long::sum);
        }
    }

    public List<PublicAchievementDTO> getPublicList(String type, String level1, String keyword, String year,
                                                     int page, int size) {
        List<PublicAchievementDTO> all = getAllFiltered(type, level1, keyword, year);
        all.sort((a, b) -> {
            LocalDate d1 = a.getAchievementDate() != null ? a.getAchievementDate() : LocalDate.MIN;
            LocalDate d2 = b.getAchievementDate() != null ? b.getAchievementDate() : LocalDate.MIN;
            return d2.compareTo(d1);
        });
        int start = (page - 1) * size;
        if (start >= all.size()) return Collections.emptyList();
        int end = Math.min(start + size, all.size());
        return all.subList(start, end);
    }

    public long countPublic(String type, String level1, String keyword, String year) {
        return getAllFiltered(type, level1, keyword, year).size();
    }

    private List<PublicAchievementDTO> getAllFiltered(String type, String level1, String keyword, String year) {
        List<PublicAchievementDTO> result = new ArrayList<>();

        if (type == null || type.isEmpty() || "COMPETITION".equals(type)) {
            for (Competition c : competitionRepository.findByStatusAndIsDeleted("ARCHIVED", 0)) {
                if (!matchLevel(level1, c.getAwardLevel())) continue;
                if (!matchKeyword(keyword, c.getCompetitionName())) continue;
                if (!matchYear(year, getYear(c.getAwardDate(), c.getApplyTime()))) continue;
                PublicAchievementDTO dto = new PublicAchievementDTO();
                dto.setType("COMPETITION"); dto.setTypeLabel("学科竞赛");
                dto.setId(c.getId()); dto.setTitle(c.getCompetitionName());
                if (c.getApplicant() != null) {
                    dto.setApplicantName(c.getApplicant().getRealName());
                    dto.setApplicantCollege(c.getApplicant().getCollege());
                }
                dto.setLevel1(c.getAwardLevel()); dto.setLevel2(c.getAwardGrade());
                dto.setAchievementDate(c.getAwardDate()); dto.setStatus(c.getStatus());
                result.add(dto);
            }
        }
        if (type == null || type.isEmpty() || "PROJECT".equals(type)) {
            for (Project p : projectRepository.findByStatusAndIsDeleted("ARCHIVED", 0)) {
                if (!matchLevel(level1, p.getProjectLevel())) continue;
                if (!matchKeyword(keyword, p.getProjectName())) continue;
                if (!matchYear(year, getYear(p.getEstablishTime(), p.getApplyTime()))) continue;
                PublicAchievementDTO dto = new PublicAchievementDTO();
                dto.setType("PROJECT"); dto.setTypeLabel("创新项目");
                dto.setId(p.getId()); dto.setTitle(p.getProjectName());
                if (p.getApplicant() != null) {
                    dto.setApplicantName(p.getApplicant().getRealName());
                    dto.setApplicantCollege(p.getApplicant().getCollege());
                }
                dto.setLevel1(p.getProjectLevel()); dto.setLevel2(p.getProjectType());
                dto.setAchievementDate(p.getEstablishTime()); dto.setStatus(p.getStatus());
                result.add(dto);
            }
        }
        if (type == null || type.isEmpty() || "PAPER".equals(type)) {
            for (Paper p : paperRepository.findByStatusAndIsDeleted("ARCHIVED", 0)) {
                if (!matchLevel(level1, p.getJournalLevel())) continue;
                if (!matchKeyword(keyword, p.getTitle())) continue;
                if (!matchYear(year, getYear(p.getAcceptanceDate(), p.getApplyTime()))) continue;
                PublicAchievementDTO dto = new PublicAchievementDTO();
                dto.setType("PAPER"); dto.setTypeLabel("学术论文");
                dto.setId(p.getId()); dto.setTitle(p.getTitle());
                if (p.getApplicant() != null) {
                    dto.setApplicantName(p.getApplicant().getRealName());
                    dto.setApplicantCollege(p.getApplicant().getCollege());
                }
                dto.setLevel1(p.getJournalLevel()); dto.setLevel2(null);
                dto.setAchievementDate(p.getAcceptanceDate()); dto.setStatus(p.getStatus());
                result.add(dto);
            }
        }
        if (type == null || type.isEmpty() || "SOFTWARE".equals(type)) {
            for (Software s : softwareRepository.findByStatusAndIsDeleted("ARCHIVED", 0)) {
                if (!matchKeyword(keyword, s.getSoftwareName())) continue;
                if (!matchYear(year, getYear(s.getRegistrationDate(), s.getApplyTime()))) continue;
                PublicAchievementDTO dto = new PublicAchievementDTO();
                dto.setType("SOFTWARE"); dto.setTypeLabel("软件著作");
                dto.setId(s.getId()); dto.setTitle(s.getSoftwareName());
                if (s.getApplicant() != null) {
                    dto.setApplicantName(s.getApplicant().getRealName());
                    dto.setApplicantCollege(s.getApplicant().getCollege());
                }
                dto.setLevel1(s.getAffiliation()); dto.setLevel2(s.getRegistrationNumber());
                dto.setAchievementDate(s.getRegistrationDate()); dto.setStatus(s.getStatus());
                result.add(dto);
            }
        }
        return result;
    }

    private boolean matchLevel(String filter, String value) {
        return filter == null || filter.isEmpty() || filter.equals(value);
    }
    private boolean matchKeyword(String filter, String value) {
        return filter == null || filter.isEmpty() || (value != null && value.toLowerCase().contains(filter.toLowerCase()));
    }
    private boolean matchYear(String filter, int year) {
        return filter == null || filter.isEmpty() || String.valueOf(year).equals(filter);
    }

    private List<DashboardStats.RecentAchievement> buildRecentItems(List<Competition> competitions,
                                                                      List<Project> projects,
                                                                      List<Paper> papers,
                                                                      List<Software> softwares) {
        List<DashboardStats.RecentAchievement> items = new ArrayList<>();
        for (Competition c : competitions) {
            DashboardStats.RecentAchievement item = new DashboardStats.RecentAchievement();
            item.setType("COMPETITION"); item.setTypeLabel("学科竞赛");
            item.setId(c.getId()); item.setTitle(c.getCompetitionName());
            item.setApplicantName(c.getApplicant() != null ? c.getApplicant().getRealName() : "");
            item.setLevel(c.getAwardLevel());
            item.setDate(c.getAwardDate() != null ? c.getAwardDate().format(DATE_FMT) : "");
            items.add(item);
        }
        for (Project p : projects) {
            DashboardStats.RecentAchievement item = new DashboardStats.RecentAchievement();
            item.setType("PROJECT"); item.setTypeLabel("创新项目");
            item.setId(p.getId()); item.setTitle(p.getProjectName());
            item.setApplicantName(p.getApplicant() != null ? p.getApplicant().getRealName() : "");
            item.setLevel(p.getProjectLevel());
            item.setDate(p.getEstablishTime() != null ? p.getEstablishTime().format(DATE_FMT) : "");
            items.add(item);
        }
        for (Paper p : papers) {
            DashboardStats.RecentAchievement item = new DashboardStats.RecentAchievement();
            item.setType("PAPER"); item.setTypeLabel("学术论文");
            item.setId(p.getId()); item.setTitle(p.getTitle());
            item.setApplicantName(p.getApplicant() != null ? p.getApplicant().getRealName() : "");
            item.setLevel(p.getJournalLevel());
            item.setDate(p.getAcceptanceDate() != null ? p.getAcceptanceDate().format(DATE_FMT) : "");
            items.add(item);
        }
        for (Software s : softwares) {
            DashboardStats.RecentAchievement item = new DashboardStats.RecentAchievement();
            item.setType("SOFTWARE"); item.setTypeLabel("软件著作");
            item.setId(s.getId()); item.setTitle(s.getSoftwareName());
            item.setApplicantName(s.getApplicant() != null ? s.getApplicant().getRealName() : "");
            item.setLevel(s.getAffiliation());
            item.setDate(s.getRegistrationDate() != null ? s.getRegistrationDate().format(DATE_FMT) : "");
            items.add(item);
        }
        items.sort((a, b) -> {
            String d1 = a.getDate() != null && !a.getDate().isEmpty() ? a.getDate() : "0000-00-00";
            String d2 = b.getDate() != null && !b.getDate().isEmpty() ? b.getDate() : "0000-00-00";
            return d2.compareTo(d1);
        });
        return items.size() > 10 ? items.subList(0, 10) : items;
    }

    private int getYear(LocalDate date, Object fallback) {
        if (date != null) return date.getYear();
        if (fallback instanceof java.time.LocalDateTime) {
            return ((java.time.LocalDateTime) fallback).getYear();
        }
        return LocalDate.now().getYear();
    }
}
