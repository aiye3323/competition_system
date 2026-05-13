package com.competition.service;

import com.competition.dto.AuditDetailDTO;
import com.competition.dto.AuditLogDTO;
import com.competition.dto.AuditRequest;
import com.competition.dto.PendingItemDTO;
import com.competition.entity.AuditLog;
import com.competition.entity.Competition;
import com.competition.entity.FileEntity;
import com.competition.entity.Paper;
import com.competition.entity.Project;
import com.competition.entity.Software;
import com.competition.entity.User;
import com.competition.repository.AuditLogRepository;
import com.competition.repository.CompetitionRepository;
import com.competition.repository.FileRepository;
import com.competition.repository.PaperRepository;
import com.competition.repository.ProjectRepository;
import com.competition.repository.SoftwareRepository;
import com.competition.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final CompetitionRepository competitionRepository;
    private final ProjectRepository projectRepository;
    private final PaperRepository paperRepository;
    private final SoftwareRepository softwareRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final NotificationService notificationService;

    @Transactional
    public AuditLogDTO secretaryApprove(Long competitionId, Long auditorId, AuditRequest request) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("竞赛记录不存在"));

        if (!"PENDING".equals(competition.getStatus())) {
            throw new RuntimeException("当前状态不可进行秘书审核");
        }

        User auditor = userRepository.findById(auditorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"SECRETARY".equals(auditor.getRole())) {
            throw new RuntimeException("仅科研秘书可进行审核");
        }

        AuditLog log = createAuditLog("COMPETITION", competitionId, auditorId, "SECRETARY", "APPROVED", request.getOpinion());

        competition.setStatus("PENDING_LEADER");
        competitionRepository.save(competition);

        // 通知申报人
        Long applicantId = competition.getApplicant().getId();
        notificationService.createNotification(
                applicantId,
                "竞赛成果已通过初审",
                "您提交的竞赛成果已通过科研秘书审核，已提交至学院领导审核。",
                competitionId
        );

        return toDTO(log);
    }

    @Transactional
    public AuditLogDTO secretaryReject(Long competitionId, Long auditorId, AuditRequest request) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("竞赛记录不存在"));

        if (!"PENDING".equals(competition.getStatus())) {
            throw new RuntimeException("当前状态不可进行秘书审核");
        }

        User auditor = userRepository.findById(auditorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"SECRETARY".equals(auditor.getRole())) {
            throw new RuntimeException("仅科研秘书可进行审核");
        }

        if (request.getOpinion() == null || request.getOpinion().trim().isEmpty()) {
            throw new RuntimeException("退回时必须填写审核意见");
        }

        AuditLog log = createAuditLog("COMPETITION", competitionId, auditorId, "SECRETARY", "REJECTED", request.getOpinion());

        competition.setStatus("REJECTED");
        competitionRepository.save(competition);

        // 通知申报人
        Long applicantId = competition.getApplicant().getId();
        notificationService.createNotification(
                applicantId,
                "竞赛成果被退回",
                "审核意见：" + request.getOpinion(),
                competitionId
        );

        return toDTO(log);
    }

    @Transactional
    public AuditLogDTO leaderApprove(Long competitionId, Long auditorId, AuditRequest request) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("竞赛记录不存在"));

        if (!"PENDING_LEADER".equals(competition.getStatus())) {
            throw new RuntimeException("当前状态不可进行领导审核");
        }

        User auditor = userRepository.findById(auditorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"LEADER".equals(auditor.getRole())) {
            throw new RuntimeException("仅学院领导可进行审核");
        }

        AuditLog log = createAuditLog("COMPETITION", competitionId, auditorId, "LEADER", "APPROVED", request.getOpinion());

        competition.setStatus("ARCHIVED");
        competition.setArchiveTime(LocalDateTime.now());
        competitionRepository.save(competition);

        // 通知申报人
        Long applicantId = competition.getApplicant().getId();
        notificationService.createNotification(
                applicantId,
                "竞赛成果已完成归档",
                "您提交的竞赛成果已通过学院领导审核，已完成归档。",
                competitionId
        );

        return toDTO(log);
    }

    @Transactional
    public AuditLogDTO leaderReject(Long competitionId, Long auditorId, AuditRequest request) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("竞赛记录不存在"));

        if (!"PENDING_LEADER".equals(competition.getStatus())) {
            throw new RuntimeException("当前状态不可进行领导审核");
        }

        User auditor = userRepository.findById(auditorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"LEADER".equals(auditor.getRole())) {
            throw new RuntimeException("仅学院领导可进行审核");
        }

        if (request.getOpinion() == null || request.getOpinion().trim().isEmpty()) {
            throw new RuntimeException("退回时必须填写审核意见");
        }

        AuditLog log = createAuditLog("COMPETITION", competitionId, auditorId, "LEADER", "REJECTED", request.getOpinion());

        competition.setStatus("REJECTED");
        competitionRepository.save(competition);

        // 通知申报人
        Long applicantId = competition.getApplicant().getId();
        notificationService.createNotification(
                applicantId,
                "最终审核未通过",
                "审核意见：" + request.getOpinion(),
                competitionId
        );

        return toDTO(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getAuditLogs(Long competitionId) {
        return getAuditLogs("COMPETITION", competitionId);
    }

    // ==================== Project 审核 ====================

    @Transactional
    public AuditLogDTO secretaryApproveProject(Long projectId, Long auditorId, AuditRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("项目记录不存在"));

        if (!"PENDING".equals(project.getStatus())) {
            throw new RuntimeException("当前状态不可进行秘书审核");
        }

        User auditor = userRepository.findById(auditorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"SECRETARY".equals(auditor.getRole())) {
            throw new RuntimeException("仅科研秘书可进行审核");
        }

        AuditLog log = createAuditLog("PROJECT", projectId, auditorId, "SECRETARY", "APPROVED", request.getOpinion());

        project.setStatus("PENDING_LEADER");
        projectRepository.save(project);

        notificationService.createNotification(
                project.getApplicant().getId(),
                "项目已通过初审",
                "您提交的创新项目\"" + project.getProjectName() + "\"已通过科研秘书审核，已提交至学院领导审核。",
                projectId
        );

        return toDTO(log);
    }

    @Transactional
    public AuditLogDTO secretaryRejectProject(Long projectId, Long auditorId, AuditRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("项目记录不存在"));

        if (!"PENDING".equals(project.getStatus())) {
            throw new RuntimeException("当前状态不可进行秘书审核");
        }

        User auditor = userRepository.findById(auditorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"SECRETARY".equals(auditor.getRole())) {
            throw new RuntimeException("仅科研秘书可进行审核");
        }

        if (request.getOpinion() == null || request.getOpinion().trim().isEmpty()) {
            throw new RuntimeException("退回时必须填写审核意见");
        }

        AuditLog log = createAuditLog("PROJECT", projectId, auditorId, "SECRETARY", "REJECTED", request.getOpinion());

        project.setStatus("REJECTED");
        projectRepository.save(project);

        notificationService.createNotification(
                project.getApplicant().getId(),
                "项目被退回",
                "审核意见：" + request.getOpinion(),
                projectId
        );

        return toDTO(log);
    }

    @Transactional
    public AuditLogDTO leaderApproveProject(Long projectId, Long auditorId, AuditRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("项目记录不存在"));

        if (!"PENDING_LEADER".equals(project.getStatus())) {
            throw new RuntimeException("当前状态不可进行领导审核");
        }

        User auditor = userRepository.findById(auditorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"LEADER".equals(auditor.getRole())) {
            throw new RuntimeException("仅学院领导可进行审核");
        }

        AuditLog log = createAuditLog("PROJECT", projectId, auditorId, "LEADER", "APPROVED", request.getOpinion());

        project.setStatus("ARCHIVED");
        project.setArchiveTime(LocalDateTime.now());
        projectRepository.save(project);

        notificationService.createNotification(
                project.getApplicant().getId(),
                "项目已完成归档",
                "您提交的创新项目\"" + project.getProjectName() + "\"已通过学院领导审核，已完成归档。",
                projectId
        );

        return toDTO(log);
    }

    @Transactional
    public AuditLogDTO leaderRejectProject(Long projectId, Long auditorId, AuditRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("项目记录不存在"));

        if (!"PENDING_LEADER".equals(project.getStatus())) {
            throw new RuntimeException("当前状态不可进行领导审核");
        }

        User auditor = userRepository.findById(auditorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"LEADER".equals(auditor.getRole())) {
            throw new RuntimeException("仅学院领导可进行审核");
        }

        if (request.getOpinion() == null || request.getOpinion().trim().isEmpty()) {
            throw new RuntimeException("退回时必须填写审核意见");
        }

        AuditLog log = createAuditLog("PROJECT", projectId, auditorId, "LEADER", "REJECTED", request.getOpinion());

        project.setStatus("REJECTED");
        projectRepository.save(project);

        notificationService.createNotification(
                project.getApplicant().getId(),
                "项目最终审核未通过",
                "审核意见：" + request.getOpinion(),
                projectId
        );

        return toDTO(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getProjectAuditLogs(Long projectId) {
        return getAuditLogs("PROJECT", projectId);
    }

    // ==================== Paper 审核 ====================

    @Transactional
    public AuditLogDTO secretaryApprovePaper(Long paperId, Long auditorId, AuditRequest request) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("论文记录不存在"));

        if (!"PENDING".equals(paper.getStatus())) {
            throw new RuntimeException("当前状态不可进行秘书审核");
        }

        User auditor = userRepository.findById(auditorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"SECRETARY".equals(auditor.getRole())) {
            throw new RuntimeException("仅科研秘书可进行审核");
        }

        AuditLog log = createAuditLog("PAPER", paperId, auditorId, "SECRETARY", "APPROVED", request.getOpinion());

        paper.setStatus("PENDING_LEADER");
        paperRepository.save(paper);

        notificationService.createNotification(
                paper.getApplicant().getId(),
                "论文已通过初审",
                "您提交的论文\"" + paper.getTitle() + "\"已通过科研秘书审核，已提交至学院领导审核。",
                paperId
        );

        return toDTO(log);
    }

    @Transactional
    public AuditLogDTO secretaryRejectPaper(Long paperId, Long auditorId, AuditRequest request) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("论文记录不存在"));

        if (!"PENDING".equals(paper.getStatus())) {
            throw new RuntimeException("当前状态不可进行秘书审核");
        }

        User auditor = userRepository.findById(auditorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"SECRETARY".equals(auditor.getRole())) {
            throw new RuntimeException("仅科研秘书可进行审核");
        }

        if (request.getOpinion() == null || request.getOpinion().trim().isEmpty()) {
            throw new RuntimeException("退回时必须填写审核意见");
        }

        AuditLog log = createAuditLog("PAPER", paperId, auditorId, "SECRETARY", "REJECTED", request.getOpinion());

        paper.setStatus("REJECTED");
        paperRepository.save(paper);

        notificationService.createNotification(
                paper.getApplicant().getId(),
                "论文被退回",
                "审核意见：" + request.getOpinion(),
                paperId
        );

        return toDTO(log);
    }

    @Transactional
    public AuditLogDTO leaderApprovePaper(Long paperId, Long auditorId, AuditRequest request) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("论文记录不存在"));

        if (!"PENDING_LEADER".equals(paper.getStatus())) {
            throw new RuntimeException("当前状态不可进行领导审核");
        }

        User auditor = userRepository.findById(auditorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"LEADER".equals(auditor.getRole())) {
            throw new RuntimeException("仅学院领导可进行审核");
        }

        AuditLog log = createAuditLog("PAPER", paperId, auditorId, "LEADER", "APPROVED", request.getOpinion());

        paper.setStatus("ARCHIVED");
        paper.setArchiveTime(LocalDateTime.now());
        paperRepository.save(paper);

        notificationService.createNotification(
                paper.getApplicant().getId(),
                "论文已完成归档",
                "您提交的论文\"" + paper.getTitle() + "\"已通过学院领导审核，已完成归档。",
                paperId
        );

        return toDTO(log);
    }

    @Transactional
    public AuditLogDTO leaderRejectPaper(Long paperId, Long auditorId, AuditRequest request) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("论文记录不存在"));

        if (!"PENDING_LEADER".equals(paper.getStatus())) {
            throw new RuntimeException("当前状态不可进行领导审核");
        }

        User auditor = userRepository.findById(auditorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"LEADER".equals(auditor.getRole())) {
            throw new RuntimeException("仅学院领导可进行审核");
        }

        if (request.getOpinion() == null || request.getOpinion().trim().isEmpty()) {
            throw new RuntimeException("退回时必须填写审核意见");
        }

        AuditLog log = createAuditLog("PAPER", paperId, auditorId, "LEADER", "REJECTED", request.getOpinion());

        paper.setStatus("REJECTED");
        paperRepository.save(paper);

        notificationService.createNotification(
                paper.getApplicant().getId(),
                "论文最终审核未通过",
                "审核意见：" + request.getOpinion(),
                paperId
        );

        return toDTO(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getPaperAuditLogs(Long paperId) {
        return getAuditLogs("PAPER", paperId);
    }

    // ==================== Software 审核 ====================

    @Transactional
    public AuditLogDTO secretaryApproveSoftware(Long softwareId, Long auditorId, AuditRequest request) {
        Software software = softwareRepository.findById(softwareId)
                .orElseThrow(() -> new RuntimeException("软著记录不存在"));

        if (!"PENDING".equals(software.getStatus())) {
            throw new RuntimeException("当前状态不可进行秘书审核");
        }

        User auditor = userRepository.findById(auditorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"SECRETARY".equals(auditor.getRole())) {
            throw new RuntimeException("仅科研秘书可进行审核");
        }

        AuditLog log = createAuditLog("SOFTWARE", softwareId, auditorId, "SECRETARY", "APPROVED", request.getOpinion());

        software.setStatus("PENDING_LEADER");
        softwareRepository.save(software);

        notificationService.createNotification(
                software.getApplicant().getId(),
                "软著已通过初审",
                "您提交的软件著作权\"" + software.getSoftwareName() + "\"已通过科研秘书审核，已提交至学院领导审核。",
                softwareId
        );

        return toDTO(log);
    }

    @Transactional
    public AuditLogDTO secretaryRejectSoftware(Long softwareId, Long auditorId, AuditRequest request) {
        Software software = softwareRepository.findById(softwareId)
                .orElseThrow(() -> new RuntimeException("软著记录不存在"));

        if (!"PENDING".equals(software.getStatus())) {
            throw new RuntimeException("当前状态不可进行秘书审核");
        }

        User auditor = userRepository.findById(auditorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"SECRETARY".equals(auditor.getRole())) {
            throw new RuntimeException("仅科研秘书可进行审核");
        }

        if (request.getOpinion() == null || request.getOpinion().trim().isEmpty()) {
            throw new RuntimeException("退回时必须填写审核意见");
        }

        AuditLog log = createAuditLog("SOFTWARE", softwareId, auditorId, "SECRETARY", "REJECTED", request.getOpinion());

        software.setStatus("REJECTED");
        softwareRepository.save(software);

        notificationService.createNotification(
                software.getApplicant().getId(),
                "软著被退回",
                "审核意见：" + request.getOpinion(),
                softwareId
        );

        return toDTO(log);
    }

    @Transactional
    public AuditLogDTO leaderApproveSoftware(Long softwareId, Long auditorId, AuditRequest request) {
        Software software = softwareRepository.findById(softwareId)
                .orElseThrow(() -> new RuntimeException("软著记录不存在"));

        if (!"PENDING_LEADER".equals(software.getStatus())) {
            throw new RuntimeException("当前状态不可进行领导审核");
        }

        User auditor = userRepository.findById(auditorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"LEADER".equals(auditor.getRole())) {
            throw new RuntimeException("仅学院领导可进行审核");
        }

        AuditLog log = createAuditLog("SOFTWARE", softwareId, auditorId, "LEADER", "APPROVED", request.getOpinion());

        software.setStatus("ARCHIVED");
        software.setArchiveTime(LocalDateTime.now());
        softwareRepository.save(software);

        notificationService.createNotification(
                software.getApplicant().getId(),
                "软著已完成归档",
                "您提交的软件著作权\"" + software.getSoftwareName() + "\"已通过学院领导审核，已完成归档。",
                softwareId
        );

        return toDTO(log);
    }

    @Transactional
    public AuditLogDTO leaderRejectSoftware(Long softwareId, Long auditorId, AuditRequest request) {
        Software software = softwareRepository.findById(softwareId)
                .orElseThrow(() -> new RuntimeException("软著记录不存在"));

        if (!"PENDING_LEADER".equals(software.getStatus())) {
            throw new RuntimeException("当前状态不可进行领导审核");
        }

        User auditor = userRepository.findById(auditorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"LEADER".equals(auditor.getRole())) {
            throw new RuntimeException("仅学院领导可进行审核");
        }

        if (request.getOpinion() == null || request.getOpinion().trim().isEmpty()) {
            throw new RuntimeException("退回时必须填写审核意见");
        }

        AuditLog log = createAuditLog("SOFTWARE", softwareId, auditorId, "LEADER", "REJECTED", request.getOpinion());

        software.setStatus("REJECTED");
        softwareRepository.save(software);

        notificationService.createNotification(
                software.getApplicant().getId(),
                "软著最终审核未通过",
                "审核意见：" + request.getOpinion(),
                softwareId
        );

        return toDTO(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getSoftwareAuditLogs(Long softwareId) {
        return getAuditLogs("SOFTWARE", softwareId);
    }

    private List<AuditLogDTO> getAuditLogs(String achievementType, Long achievementId) {
        List<AuditLog> logs = auditLogRepository
                .findByAchievementTypeAndAchievementIdOrderByAuditTimeAsc(achievementType, achievementId);
        return logs.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private AuditLog createAuditLog(String achievementType, Long achievementId, Long auditorId,
                                    String auditorRole, String result, String opinion) {
        AuditLog log = new AuditLog();
        log.setAchievementType(achievementType);
        log.setAchievementId(achievementId);
        log.setAuditorId(auditorId);
        log.setAuditorRole(auditorRole);
        log.setResult(result);
        log.setOpinion(opinion);
        log.setAuditTime(LocalDateTime.now());
        return auditLogRepository.save(log);
    }

    private AuditLogDTO toDTO(AuditLog log) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setId(log.getId());
        dto.setAchievementType(log.getAchievementType());
        dto.setAchievementId(log.getAchievementId());
        dto.setAuditorId(log.getAuditorId());
        dto.setAuditorRole(log.getAuditorRole());
        dto.setResult(log.getResult());
        dto.setOpinion(log.getOpinion());
        dto.setAuditTime(log.getAuditTime());

        userRepository.findById(log.getAuditorId()).ifPresent(u ->
                dto.setAuditorName(u.getRealName()));

        return dto;
    }

    // ==================== 统一审核列表 ====================

    @Transactional(readOnly = true)
    public Page<PendingItemDTO> getPendingList(String role, String type, String status, String keyword, int page, int size) {
        // Determine target status from role if not explicitly provided
        String targetStatus = status;
        if (targetStatus == null || targetStatus.isEmpty()) {
            if ("SECRETARY".equals(role)) {
                targetStatus = "PENDING";
            } else if ("LEADER".equals(role)) {
                targetStatus = "PENDING_LEADER";
            }
        }

        List<PendingItemDTO> allItems = new ArrayList<>();

        boolean fetchAll = "ALL".equals(type) || type == null || type.isEmpty();

        if (fetchAll || "COMPETITION".equals(type)) {
            List<Competition> list = competitionRepository
                .findByStatusAndIsDeletedOrderByApplyTimeDesc(targetStatus, 0, Pageable.unpaged());
            allItems.addAll(list.stream()
                .filter(c -> keyword == null || keyword.isEmpty() || c.getCompetitionName().contains(keyword) || c.getApplicant().getRealName().contains(keyword))
                .map(c -> toPendingItem("COMPETITION", c.getId(), c.getCompetitionName(), c.getApplicant(), c.getApplyTime(), c.getStatus(), c.getCategory()))
                .toList());
        }
        if (fetchAll || "PROJECT".equals(type)) {
            List<Project> list = projectRepository
                .findByStatusAndIsDeletedOrderByApplyTimeDesc(targetStatus, 0, Pageable.unpaged());
            allItems.addAll(list.stream()
                .filter(p -> keyword == null || keyword.isEmpty() || p.getProjectName().contains(keyword) || p.getApplicant().getRealName().contains(keyword))
                .map(p -> toPendingItem("PROJECT", p.getId(), p.getProjectName(), p.getApplicant(), p.getApplyTime(), p.getStatus(), p.getProjectLevel()))
                .toList());
        }
        if (fetchAll || "PAPER".equals(type)) {
            List<Paper> list = paperRepository
                .findByStatusAndIsDeletedOrderByApplyTimeDesc(targetStatus, 0, Pageable.unpaged());
            allItems.addAll(list.stream()
                .filter(p -> keyword == null || keyword.isEmpty() || p.getTitle().contains(keyword) || p.getApplicant().getRealName().contains(keyword))
                .map(p -> toPendingItem("PAPER", p.getId(), p.getTitle(), p.getApplicant(), p.getApplyTime(), p.getStatus(), p.getJournalLevel()))
                .toList());
        }
        if (fetchAll || "SOFTWARE".equals(type)) {
            List<Software> list = softwareRepository
                .findByStatusAndIsDeletedOrderByApplyTimeDesc(targetStatus, 0, Pageable.unpaged());
            allItems.addAll(list.stream()
                .filter(s -> keyword == null || keyword.isEmpty() || s.getSoftwareName().contains(keyword) || s.getApplicant().getRealName().contains(keyword))
                .map(s -> toPendingItem("SOFTWARE", s.getId(), s.getSoftwareName(), s.getApplicant(), s.getApplyTime(), s.getStatus(), s.getAffiliation()))
                .toList());
        }

        // Sort by applyTime descending
        allItems.sort((a, b) -> b.getApplyTime().compareTo(a.getApplyTime()));

        // Manual pagination
        int start = (page - 1) * size;
        if (start >= allItems.size()) {
            return new PageImpl<>(List.of(), PageRequest.of(page - 1, size), allItems.size());
        }
        int end = Math.min(start + size, allItems.size());
        List<PendingItemDTO> pageItems = allItems.subList(start, end);

        return new PageImpl<>(pageItems, PageRequest.of(page - 1, size), allItems.size());
    }

    private PendingItemDTO toPendingItem(String type, Long id, String title, User applicant, LocalDateTime applyTime, String status, String extraInfo) {
        PendingItemDTO dto = new PendingItemDTO();
        dto.setId(id);
        dto.setType(type);
        dto.setTitle(title);
        dto.setApplicantName(applicant != null ? applicant.getRealName() : "");
        dto.setApplicantId(applicant != null ? applicant.getId() : null);
        dto.setApplyTime(applyTime);
        dto.setStatus(status);
        dto.setExtraInfo(extraInfo);
        return dto;
    }

    // ==================== 统一审核详情 ====================

    @Transactional(readOnly = true)
    public AuditDetailDTO getAuditDetail(String type, Long id) {
        AuditDetailDTO dto = new AuditDetailDTO();
        dto.setType(type);
        dto.setId(id);

        Map<String, Object> fields = new LinkedHashMap<>();

        switch (type) {
            case "COMPETITION" -> {
                Competition c = competitionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("竞赛记录不存在"));
                dto.setTitle(c.getCompetitionName());
                dto.setApplicantName(c.getApplicant().getRealName());
                dto.setApplicantId(c.getApplicant().getId());
                dto.setApplyTime(c.getApplyTime());
                dto.setStatus(c.getStatus());
                fields.put("competitionName", c.getCompetitionName());
                fields.put("category", c.getCategory());
                fields.put("awardLevel", c.getAwardLevel());
                fields.put("awardGrade", c.getAwardGrade());
                fields.put("awardUnit", c.getAwardUnit());
                fields.put("advisor", c.getAdvisor());
                fields.put("participants", c.getParticipants());
                fields.put("organizer", c.getOrganizer());
                fields.put("awardDate", c.getAwardDate());
            }
            case "PROJECT" -> {
                Project p = projectRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("项目记录不存在"));
                dto.setTitle(p.getProjectName());
                dto.setApplicantName(p.getApplicant().getRealName());
                dto.setApplicantId(p.getApplicant().getId());
                dto.setApplyTime(p.getApplyTime());
                dto.setStatus(p.getStatus());
                fields.put("projectName", p.getProjectName());
                fields.put("projectLevel", p.getProjectLevel());
                fields.put("projectType", p.getProjectType());
                fields.put("advisor", p.getAdvisor());
                fields.put("members", p.getMembers());
                fields.put("establishTime", p.getEstablishTime());
            }
            case "PAPER" -> {
                Paper p = paperRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("论文记录不存在"));
                dto.setTitle(p.getTitle());
                dto.setApplicantName(p.getApplicant().getRealName());
                dto.setApplicantId(p.getApplicant().getId());
                dto.setApplyTime(p.getApplyTime());
                dto.setStatus(p.getStatus());
                fields.put("title", p.getTitle());
                fields.put("journalName", p.getJournalName());
                fields.put("journalLevel", p.getJournalLevel());
                fields.put("keywords", p.getKeywords());
                fields.put("authors", p.getAuthors());
                fields.put("submissionDate", p.getSubmissionDate());
                fields.put("acceptanceDate", p.getAcceptanceDate());
            }
            case "SOFTWARE" -> {
                Software s = softwareRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("软件记录不存在"));
                dto.setTitle(s.getSoftwareName());
                dto.setApplicantName(s.getApplicant().getRealName());
                dto.setApplicantId(s.getApplicant().getId());
                dto.setApplyTime(s.getApplyTime());
                dto.setStatus(s.getStatus());
                fields.put("softwareName", s.getSoftwareName());
                fields.put("affiliation", s.getAffiliation());
                fields.put("copyrightOwner", s.getCopyrightOwner());
                fields.put("registrationNumber", s.getRegistrationNumber());
                fields.put("registrationDate", s.getRegistrationDate());
            }
        }

        dto.setFields(fields);

        // Files
        List<FileEntity> fileEntities = fileRepository.findByRelatedTypeAndRelatedId(type, id);
        List<AuditDetailDTO.FileInfo> fileInfos = fileEntities.stream().map(f -> {
            AuditDetailDTO.FileInfo fi = new AuditDetailDTO.FileInfo();
            fi.setId(f.getId());
            fi.setOriginalName(f.getOriginalName());
            fi.setFileType(f.getFileType());
            fi.setFileSize(f.getFileSize());
            return fi;
        }).toList();
        dto.setFiles(fileInfos);

        // Audit logs
        List<AuditLogDTO> logs = getAuditLogs(type, id);
        dto.setAuditLogs(logs);

        return dto;
    }
}
