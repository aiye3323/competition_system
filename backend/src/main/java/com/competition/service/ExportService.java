package com.competition.service;

import com.competition.entity.Competition;
import com.competition.entity.Paper;
import com.competition.entity.Project;
import com.competition.entity.Software;
import com.competition.repository.CompetitionRepository;
import com.competition.repository.PaperRepository;
import com.competition.repository.ProjectRepository;
import com.competition.repository.SoftwareRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final CompetitionRepository competitionRepository;
    private final ProjectRepository projectRepository;
    private final PaperRepository paperRepository;
    private final SoftwareRepository softwareRepository;

    private static final String[] UNIFIED_HEADERS = {"序号", "成果类型", "名称", "申报人", "所在学院", "级别", "二级分类", "日期", "状态"};

    // ──────────────────────────────────────────────
    // 各类型导出（含角色过滤）
    // ──────────────────────────────────────────────

    public byte[] exportCompetitions(String category, String awardLevel, String status,
                                     Long userId, String role) {
        Specification<Competition> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), 0));
            if (category != null && !category.isEmpty())
                predicates.add(cb.equal(root.get("category"), category));
            if (awardLevel != null && !awardLevel.isEmpty())
                predicates.add(cb.equal(root.get("awardLevel"), awardLevel));
            if (status != null && !status.isEmpty())
                predicates.add(cb.equal(root.get("status"), status));
            if ("STUDENT".equals(role) || "TEACHER".equals(role))
                predicates.add(cb.equal(root.get("applicant").get("id"), userId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        List<Competition> list = competitionRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "applyTime"));

        String[] headers = {"序号", "竞赛名称", "类别", "获奖级别", "获奖等级", "颁奖单位", "主办单位", "承办单位", "获奖时间", "指导教师", "参赛选手", "申报人", "审核状态"};
        return buildExcel("学科竞赛", headers, list.size(), (wb, sheet, rowNum) -> {
            for (int i = 0; i < list.size(); i++) {
                Competition c = list.get(i);
                Row row = sheet.createRow(rowNum + i);
                Object[] vals = {
                        rowNum + i, c.getCompetitionName(), c.getCategory(), c.getAwardLevel(),
                        c.getAwardGrade(), c.getAwardUnit(), c.getOrganizer(), c.getCoOrganizer(),
                        c.getAwardDate(), c.getAdvisor(), c.getParticipants(),
                        c.getApplicant() != null ? c.getApplicant().getRealName() : "",
                        statusLabel(c.getStatus())
                };
                fillRow(row, vals, wb);
            }
        });
    }

    public byte[] exportProjects(String projectLevel, String status,
                                 Long userId, String role) {
        Specification<Project> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), 0));
            if (projectLevel != null && !projectLevel.isEmpty())
                predicates.add(cb.equal(root.get("projectLevel"), projectLevel));
            if (status != null && !status.isEmpty())
                predicates.add(cb.equal(root.get("status"), status));
            if ("STUDENT".equals(role) || "TEACHER".equals(role))
                predicates.add(cb.equal(root.get("applicant").get("id"), userId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        List<Project> list = projectRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "applyTime"));

        String[] headers = {"序号", "项目名称", "立项级别", "立项类型", "指导教师", "立项人员", "立项时间", "申报人", "审核状态"};
        return buildExcel("创新项目", headers, list.size(), (wb, sheet, rowNum) -> {
            for (int i = 0; i < list.size(); i++) {
                Project p = list.get(i);
                Row row = sheet.createRow(rowNum + i);
                Object[] vals = {
                        rowNum + i, p.getProjectName(), p.getProjectLevel(), p.getProjectType(),
                        p.getAdvisor(), p.getMembers(), p.getEstablishTime(),
                        p.getApplicant() != null ? p.getApplicant().getRealName() : "",
                        statusLabel(p.getStatus())
                };
                fillRow(row, vals, wb);
            }
        });
    }

    public byte[] exportPapers(String journalLevel, String status,
                               Long userId, String role) {
        Specification<Paper> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), 0));
            if (journalLevel != null && !journalLevel.isEmpty())
                predicates.add(cb.equal(root.get("journalLevel"), journalLevel));
            if (status != null && !status.isEmpty())
                predicates.add(cb.equal(root.get("status"), status));
            if ("STUDENT".equals(role) || "TEACHER".equals(role))
                predicates.add(cb.equal(root.get("applicant").get("id"), userId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        List<Paper> list = paperRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "applyTime"));

        String[] headers = {"序号", "论文标题", "期刊/会议名称", "期刊级别", "关键词", "作者列表", "投稿日期", "录用日期", "申报人", "审核状态"};
        return buildExcel("学术论文", headers, list.size(), (wb, sheet, rowNum) -> {
            for (int i = 0; i < list.size(); i++) {
                Paper p = list.get(i);
                Row row = sheet.createRow(rowNum + i);
                Object[] vals = {
                        rowNum + i, p.getTitle(), p.getJournalName(), p.getJournalLevel(),
                        p.getKeywords(), p.getAuthors(), p.getSubmissionDate(), p.getAcceptanceDate(),
                        p.getApplicant() != null ? p.getApplicant().getRealName() : "",
                        statusLabel(p.getStatus())
                };
                fillRow(row, vals, wb);
            }
        });
    }

    public byte[] exportSoftware(String registrationNumber, String status,
                                 Long userId, String role) {
        Specification<Software> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), 0));
            if (registrationNumber != null && !registrationNumber.isEmpty())
                predicates.add(cb.like(root.get("registrationNumber"), "%" + registrationNumber + "%"));
            if (status != null && !status.isEmpty())
                predicates.add(cb.equal(root.get("status"), status));
            if ("STUDENT".equals(role) || "TEACHER".equals(role))
                predicates.add(cb.equal(root.get("applicant").get("id"), userId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        List<Software> list = softwareRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "applyTime"));

        String[] headers = {"序号", "软件名称", "所属单位", "著作权人", "登记号", "登记日期", "申报人", "审核状态"};
        return buildExcel("软件著作权", headers, list.size(), (wb, sheet, rowNum) -> {
            for (int i = 0; i < list.size(); i++) {
                Software s = list.get(i);
                Row row = sheet.createRow(rowNum + i);
                Object[] vals = {
                        rowNum + i, s.getSoftwareName(), s.getAffiliation(), s.getCopyrightOwner(),
                        s.getRegistrationNumber(), s.getRegistrationDate(),
                        s.getApplicant() != null ? s.getApplicant().getRealName() : "",
                        statusLabel(s.getStatus())
                };
                fillRow(row, vals, wb);
            }
        });
    }

    private String statusLabel(String status) {
        if (status == null) return "";
        java.util.Map<String, String> map = java.util.Map.of(
                "DRAFT", "草稿", "PENDING", "待审核", "PENDING_LEADER", "领导审核中",
                "APPROVED", "已通过", "REJECTED", "已驳回", "ARCHIVED", "已归档");
        return map.getOrDefault(status, status);
    }

    @FunctionalInterface
    private interface SheetWriter {
        void write(SXSSFWorkbook wb, Sheet sheet, int startRow);
    }

    private byte[] buildExcel(String sheetName, String[] headers, int dataSize, SheetWriter writer) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
            CellStyle headerStyle = createHeaderStyle(wb);
            Sheet sheet = wb.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            writer.write(wb, sheet, 1);
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 256 * 18);
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            wb.dispose();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("导出Excel失败: " + e.getMessage(), e);
        }
    }

    private void fillRow(Row row, Object[] vals, SXSSFWorkbook wb) {
        CellStyle style = createCellStyle(wb);
        CellStyle dateStyle = createDateStyle(wb);
        for (int j = 0; j < vals.length; j++) {
            Cell cell = row.createCell(j);
            cell.setCellStyle(style);
            Object val = vals[j];
            if (val instanceof Number) {
                cell.setCellValue(((Number) val).doubleValue());
            } else if (val instanceof LocalDate) {
                cell.setCellValue(val.toString());
                cell.setCellStyle(dateStyle);
            } else {
                cell.setCellValue(val != null ? val.toString() : "");
            }
        }
    }

    // ──────────────────────────────────────────────
    // 公开成果导出（已有逻辑）
    // ──────────────────────────────────────────────

    public byte[] exportPublicAchievements(String type, String level1, String keyword, String year) {
        List<Object[]> rows = collectRows(type, level1, keyword, year);

        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle dateStyle = createDateStyle(wb);
            CellStyle cellStyle = createCellStyle(wb);

            // Sheet 1: 全院成果汇总
            Sheet sheet = wb.createSheet("全院成果汇总");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < UNIFIED_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(UNIFIED_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < rows.size(); i++) {
                Object[] row = rows.get(i);
                Row dataRow = sheet.createRow(i + 1);
                for (int j = 0; j < row.length; j++) {
                    Cell cell = dataRow.createCell(j);
                    cell.setCellStyle(cellStyle);
                    Object val = row[j];
                    if (val instanceof Number) {
                        cell.setCellValue(((Number) val).doubleValue());
                    } else if (val instanceof LocalDate) {
                        cell.setCellValue(val.toString());
                        cell.setCellStyle(dateStyle);
                    } else {
                        cell.setCellValue(val != null ? val.toString() : "");
                    }
                }
            }

            // Auto-size columns
            for (int i = 0; i < UNIFIED_HEADERS.length; i++) {
                sheet.setColumnWidth(i, 256 * 18);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            wb.dispose();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("导出Excel失败: " + e.getMessage(), e);
        }
    }

    private List<Object[]> collectRows(String type, String level1, String keyword, String year) {
        List<Object[]> rows = new ArrayList<>();

        if (type == null || type.isEmpty() || "COMPETITION".equals(type)) {
            for (Competition c : competitionRepository.findByStatusAndIsDeleted("ARCHIVED", 0)) {
                if (!matchFilter(level1, c.getAwardLevel())) continue;
                if (!matchKeyword(keyword, c.getCompetitionName())) continue;
                if (!matchYear(year, getYear(c.getAwardDate(), c.getApplyTime()))) continue;
                rows.add(new Object[]{
                        rows.size() + 1,
                        "学科竞赛",
                        c.getCompetitionName(),
                        c.getApplicant() != null ? c.getApplicant().getRealName() : "",
                        c.getApplicant() != null ? c.getApplicant().getCollege() : "",
                        c.getAwardLevel(),
                        c.getAwardGrade(),
                        c.getAwardDate(),
                        "已归档"
                });
            }
        }

        if (type == null || type.isEmpty() || "PROJECT".equals(type)) {
            for (Project p : projectRepository.findByStatusAndIsDeleted("ARCHIVED", 0)) {
                if (!matchFilter(level1, p.getProjectLevel())) continue;
                if (!matchKeyword(keyword, p.getProjectName())) continue;
                if (!matchYear(year, getYear(p.getEstablishTime(), p.getApplyTime()))) continue;
                rows.add(new Object[]{
                        rows.size() + 1,
                        "创新项目",
                        p.getProjectName(),
                        p.getApplicant() != null ? p.getApplicant().getRealName() : "",
                        p.getApplicant() != null ? p.getApplicant().getCollege() : "",
                        p.getProjectLevel(),
                        p.getProjectType(),
                        p.getEstablishTime(),
                        "已归档"
                });
            }
        }

        if (type == null || type.isEmpty() || "PAPER".equals(type)) {
            for (Paper p : paperRepository.findByStatusAndIsDeleted("ARCHIVED", 0)) {
                if (!matchFilter(level1, p.getJournalLevel())) continue;
                if (!matchKeyword(keyword, p.getTitle())) continue;
                if (!matchYear(year, getYear(p.getAcceptanceDate(), p.getApplyTime()))) continue;
                rows.add(new Object[]{
                        rows.size() + 1,
                        "学术论文",
                        p.getTitle(),
                        p.getApplicant() != null ? p.getApplicant().getRealName() : "",
                        p.getApplicant() != null ? p.getApplicant().getCollege() : "",
                        p.getJournalLevel(),
                        "",
                        p.getAcceptanceDate(),
                        "已归档"
                });
            }
        }
        if (type == null || type.isEmpty() || "SOFTWARE".equals(type)) {
            for (Software s : softwareRepository.findByStatusAndIsDeleted("ARCHIVED", 0)) {
                if (!matchKeyword(keyword, s.getSoftwareName())) continue;
                if (!matchYear(year, getYear(s.getRegistrationDate(), s.getApplyTime()))) continue;
                rows.add(new Object[]{
                        rows.size() + 1,
                        "软件著作",
                        s.getSoftwareName(),
                        s.getApplicant() != null ? s.getApplicant().getRealName() : "",
                        s.getApplicant() != null ? s.getApplicant().getCollege() : "",
                        s.getAffiliation(),
                        s.getRegistrationNumber(),
                        s.getRegistrationDate(),
                        "已归档"
                });
            }
        }
        return rows;
    }

    private boolean matchFilter(String filter, String value) {
        return filter == null || filter.isEmpty() || filter.equals(value);
    }

    private boolean matchKeyword(String filter, String value) {
        return filter == null || filter.isEmpty() || (value != null && value.toLowerCase().contains(filter.toLowerCase()));
    }

    private boolean matchYear(String filter, int year) {
        return filter == null || filter.isEmpty() || String.valueOf(year).equals(filter);
    }

    private int getYear(LocalDate date, LocalDateTime fallback) {
        if (date != null) return date.getYear();
        if (fallback != null) return fallback.getYear();
        return LocalDate.now().getYear();
    }

    private CellStyle createHeaderStyle(SXSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createCellStyle(SXSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createDateStyle(SXSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        CreationHelper helper = wb.getCreationHelper();
        style.setDataFormat(helper.createDataFormat().getFormat("yyyy\"年\"mm\"月\"dd\"日\""));
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
}
