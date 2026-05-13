package com.competition.controller;

import com.competition.dto.ProjectDTO;
import com.competition.dto.ProjectSubmitRequest;
import com.competition.dto.Result;
import com.competition.service.ExportService;
import com.competition.service.ProjectService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ExportService exportService;

    public ProjectController(ProjectService projectService, ExportService exportService) {
        this.projectService = projectService;
        this.exportService = exportService;
    }

    @PostMapping
    public Result<ProjectDTO> submit(@RequestBody ProjectSubmitRequest request,
                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        ProjectDTO dto = projectService.submit(request, userId);
        return Result.success(dto);
    }

    @GetMapping
    public Result<Page<ProjectDTO>> list(
            @RequestParam(required = false) String projectLevel,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst().map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .orElse("STUDENT");

        Page<ProjectDTO> result = projectService.list(projectLevel, status, page, size, userId, role);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<ProjectDTO> detail(@PathVariable Long id) {
        ProjectDTO dto = projectService.detail(id);
        return Result.success(dto);
    }

    @PutMapping("/{id}")
    public Result<ProjectDTO> update(@PathVariable Long id,
                                      @RequestBody ProjectSubmitRequest request,
                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        ProjectDTO dto = projectService.update(id, request, userId);
        return Result.success(dto);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        projectService.delete(id, userId);
        return Result.success(null);
    }

    @GetMapping("/pending")
    public Result<Page<ProjectDTO>> pending(
            @RequestParam(defaultValue = "SECRETARY") String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ProjectDTO> result = projectService.listPending(role, page, size);
        return Result.success(result);
    }

    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) String projectLevel,
            @RequestParam(required = false) String status,
            Authentication authentication,
            HttpServletResponse response) throws Exception {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst().map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .orElse("STUDENT");
        byte[] data = exportService.exportProjects(projectLevel, status, userId, role);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode("创新项目.xlsx", StandardCharsets.UTF_8));
        try (OutputStream os = response.getOutputStream()) {
            os.write(data);
            os.flush();
        }
    }
}
