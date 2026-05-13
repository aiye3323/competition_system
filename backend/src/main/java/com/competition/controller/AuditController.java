package com.competition.controller;

import com.competition.dto.AuditDetailDTO;
import com.competition.dto.AuditLogDTO;
import com.competition.dto.AuditRequest;
import com.competition.dto.PendingItemDTO;
import com.competition.dto.Result;
import com.competition.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping("/secretary/approve/{competitionId}")
    public Result<AuditLogDTO> secretaryApprove(@PathVariable Long competitionId,
                                                 @RequestBody AuditRequest request,
                                                 Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AuditLogDTO dto = auditService.secretaryApprove(competitionId, userId, request);
        return Result.success(dto);
    }

    @PostMapping("/secretary/reject/{competitionId}")
    public Result<AuditLogDTO> secretaryReject(@PathVariable Long competitionId,
                                                @RequestBody AuditRequest request,
                                                Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AuditLogDTO dto = auditService.secretaryReject(competitionId, userId, request);
        return Result.success(dto);
    }

    @PostMapping("/leader/approve/{competitionId}")
    public Result<AuditLogDTO> leaderApprove(@PathVariable Long competitionId,
                                              @RequestBody AuditRequest request,
                                              Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AuditLogDTO dto = auditService.leaderApprove(competitionId, userId, request);
        return Result.success(dto);
    }

    @PostMapping("/leader/reject/{competitionId}")
    public Result<AuditLogDTO> leaderReject(@PathVariable Long competitionId,
                                             @RequestBody AuditRequest request,
                                             Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AuditLogDTO dto = auditService.leaderReject(competitionId, userId, request);
        return Result.success(dto);
    }

    @GetMapping("/competition/{competitionId}")
    public Result<List<AuditLogDTO>> getAuditLogs(@PathVariable Long competitionId) {
        List<AuditLogDTO> logs = auditService.getAuditLogs(competitionId);
        return Result.success(logs);
    }

    // ==================== Project 审核 ====================

    @PostMapping("/secretary/project/{projectId}/approve")
    public Result<AuditLogDTO> secretaryApproveProject(@PathVariable Long projectId,
                                                        @RequestBody AuditRequest request,
                                                        Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AuditLogDTO dto = auditService.secretaryApproveProject(projectId, userId, request);
        return Result.success(dto);
    }

    @PostMapping("/secretary/project/{projectId}/reject")
    public Result<AuditLogDTO> secretaryRejectProject(@PathVariable Long projectId,
                                                       @RequestBody AuditRequest request,
                                                       Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AuditLogDTO dto = auditService.secretaryRejectProject(projectId, userId, request);
        return Result.success(dto);
    }

    @PostMapping("/leader/project/{projectId}/approve")
    public Result<AuditLogDTO> leaderApproveProject(@PathVariable Long projectId,
                                                     @RequestBody AuditRequest request,
                                                     Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AuditLogDTO dto = auditService.leaderApproveProject(projectId, userId, request);
        return Result.success(dto);
    }

    @PostMapping("/leader/project/{projectId}/reject")
    public Result<AuditLogDTO> leaderRejectProject(@PathVariable Long projectId,
                                                    @RequestBody AuditRequest request,
                                                    Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AuditLogDTO dto = auditService.leaderRejectProject(projectId, userId, request);
        return Result.success(dto);
    }

    @GetMapping("/project/{projectId}")
    public Result<List<AuditLogDTO>> getProjectAuditLogs(@PathVariable Long projectId) {
        List<AuditLogDTO> logs = auditService.getProjectAuditLogs(projectId);
        return Result.success(logs);
    }

    // ==================== Paper 审核 ====================

    @PostMapping("/secretary/paper/{paperId}/approve")
    public Result<AuditLogDTO> secretaryApprovePaper(@PathVariable Long paperId,
                                                      @RequestBody AuditRequest request,
                                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AuditLogDTO dto = auditService.secretaryApprovePaper(paperId, userId, request);
        return Result.success(dto);
    }

    @PostMapping("/secretary/paper/{paperId}/reject")
    public Result<AuditLogDTO> secretaryRejectPaper(@PathVariable Long paperId,
                                                     @RequestBody AuditRequest request,
                                                     Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AuditLogDTO dto = auditService.secretaryRejectPaper(paperId, userId, request);
        return Result.success(dto);
    }

    @PostMapping("/leader/paper/{paperId}/approve")
    public Result<AuditLogDTO> leaderApprovePaper(@PathVariable Long paperId,
                                                   @RequestBody AuditRequest request,
                                                   Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AuditLogDTO dto = auditService.leaderApprovePaper(paperId, userId, request);
        return Result.success(dto);
    }

    @PostMapping("/leader/paper/{paperId}/reject")
    public Result<AuditLogDTO> leaderRejectPaper(@PathVariable Long paperId,
                                                  @RequestBody AuditRequest request,
                                                  Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AuditLogDTO dto = auditService.leaderRejectPaper(paperId, userId, request);
        return Result.success(dto);
    }

    @GetMapping("/paper/{paperId}")
    public Result<List<AuditLogDTO>> getPaperAuditLogs(@PathVariable Long paperId) {
        List<AuditLogDTO> logs = auditService.getPaperAuditLogs(paperId);
        return Result.success(logs);
    }

    // ==================== Software 审核 ====================

    @PostMapping("/secretary/software/{softwareId}/approve")
    public Result<AuditLogDTO> secretaryApproveSoftware(@PathVariable Long softwareId,
                                                        @RequestBody AuditRequest request,
                                                        Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(auditService.secretaryApproveSoftware(softwareId, userId, request));
    }

    @PostMapping("/secretary/software/{softwareId}/reject")
    public Result<AuditLogDTO> secretaryRejectSoftware(@PathVariable Long softwareId,
                                                       @RequestBody AuditRequest request,
                                                       Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(auditService.secretaryRejectSoftware(softwareId, userId, request));
    }

    @PostMapping("/leader/software/{softwareId}/approve")
    public Result<AuditLogDTO> leaderApproveSoftware(@PathVariable Long softwareId,
                                                     @RequestBody AuditRequest request,
                                                     Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(auditService.leaderApproveSoftware(softwareId, userId, request));
    }

    @PostMapping("/leader/software/{softwareId}/reject")
    public Result<AuditLogDTO> leaderRejectSoftware(@PathVariable Long softwareId,
                                                    @RequestBody AuditRequest request,
                                                    Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(auditService.leaderRejectSoftware(softwareId, userId, request));
    }

    @GetMapping("/software/{softwareId}")
    public Result<List<AuditLogDTO>> getSoftwareAuditLogs(@PathVariable Long softwareId) {
        List<AuditLogDTO> logs = auditService.getSoftwareAuditLogs(softwareId);
        return Result.success(logs);
    }

    // ==================== 统一审核接口 ====================

    @GetMapping("/pending-list")
    public Result<Page<PendingItemDTO>> getPendingList(
            @RequestParam(defaultValue = "SECRETARY") String role,
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(auditService.getPendingList(role, type, status, keyword, page, size));
    }

    @GetMapping("/detail/{type}/{id}")
    public Result<AuditDetailDTO> getAuditDetail(@PathVariable String type, @PathVariable Long id) {
        return Result.success(auditService.getAuditDetail(type, id));
    }
}
