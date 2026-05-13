package com.competition.controller;

import com.competition.dto.Result;
import com.competition.dto.SoftwareDTO;
import com.competition.dto.SoftwareSubmitRequest;
import com.competition.service.ExportService;
import com.competition.service.SoftwareService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/software")
public class SoftwareController {

    private final SoftwareService softwareService;
    private final ExportService exportService;

    public SoftwareController(SoftwareService softwareService, ExportService exportService) {
        this.softwareService = softwareService;
        this.exportService = exportService;
    }

    @PostMapping
    public Result<SoftwareDTO> submit(@RequestBody SoftwareSubmitRequest request,
                                       Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(softwareService.submit(request, userId));
    }

    @GetMapping
    public Result<Page<SoftwareDTO>> list(
            @RequestParam(required = false) String registrationNumber,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst().map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .orElse("STUDENT");
        return Result.success(softwareService.list(registrationNumber, status, page, size, userId, role));
    }

    @GetMapping("/{id}")
    public Result<SoftwareDTO> detail(@PathVariable Long id) {
        return Result.success(softwareService.detail(id));
    }

    @PutMapping("/{id}")
    public Result<SoftwareDTO> update(@PathVariable Long id,
                                       @RequestBody SoftwareSubmitRequest request,
                                       Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(softwareService.update(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        softwareService.delete(id, userId);
        return Result.success(null);
    }

    @GetMapping("/pending")
    public Result<Page<SoftwareDTO>> pending(
            @RequestParam(defaultValue = "SECRETARY") String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(softwareService.listPending(role, page, size));
    }

    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) String registrationNumber,
            @RequestParam(required = false) String status,
            Authentication authentication,
            HttpServletResponse response) throws Exception {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst().map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .orElse("STUDENT");
        byte[] data = exportService.exportSoftware(registrationNumber, status, userId, role);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode("软件著作权.xlsx", StandardCharsets.UTF_8));
        try (OutputStream os = response.getOutputStream()) {
            os.write(data);
            os.flush();
        }
    }
}
