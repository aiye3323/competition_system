package com.competition.controller;

import com.competition.dto.CompetitionDTO;
import com.competition.dto.CompetitionSubmitRequest;
import com.competition.dto.Result;
import com.competition.service.CompetitionService;
import com.competition.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/competitions")
public class CompetitionController {

    private final CompetitionService competitionService;
    private final ExportService exportService;

    // 手动构造器注入，彻底解决未初始化报错
    public CompetitionController(CompetitionService competitionService, ExportService exportService) {
        this.competitionService = competitionService;
        this.exportService = exportService;
    }

    @PostMapping
    public Result<CompetitionDTO> submit(@RequestBody CompetitionSubmitRequest request,
                                         Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        CompetitionDTO dto = competitionService.submit(request, userId);
        return Result.success(dto);
    }

    @GetMapping
    public Result<Page<CompetitionDTO>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String awardLevel,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst().map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .orElse("STUDENT");

        Page<CompetitionDTO> result = competitionService.list(category, awardLevel, status, page, size, userId, role);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<CompetitionDTO> detail(@PathVariable Long id) {
        CompetitionDTO dto = competitionService.detail(id);
        return Result.success(dto);
    }

    @PutMapping("/{id}")
    public Result<CompetitionDTO> update(@PathVariable Long id,
                                         @RequestBody CompetitionSubmitRequest request,
                                         Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        CompetitionDTO dto = competitionService.update(id, request, userId);
        return Result.success(dto);
    }

    @GetMapping("/pending")
    public Result<Page<CompetitionDTO>> pending(
            @RequestParam(defaultValue = "SECRETARY") String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<CompetitionDTO> result = competitionService.listPending(role, page, size);
        return Result.success(result);
    }

    @GetMapping("/audited")
    public Result<Page<CompetitionDTO>> audited(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<CompetitionDTO> result = competitionService.listAudited(page, size);
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        competitionService.delete(id, userId);
        return Result.success(null);
    }

    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String awardLevel,
            @RequestParam(required = false) String status,
            Authentication authentication,
            HttpServletResponse response) throws Exception {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst().map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .orElse("STUDENT");

        byte[] data = exportService.exportCompetitions(category, awardLevel, status, userId, role);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode("学科竞赛.xlsx", StandardCharsets.UTF_8));
        try (OutputStream os = response.getOutputStream()) {
            os.write(data);
            os.flush();
        }
    }
}