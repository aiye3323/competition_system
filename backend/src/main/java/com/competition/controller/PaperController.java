package com.competition.controller;

import com.competition.dto.PaperDTO;
import com.competition.dto.PaperSubmitRequest;
import com.competition.dto.Result;
import com.competition.service.ExportService;
import com.competition.service.PaperService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/papers")
public class PaperController {

    private final PaperService paperService;
    private final ExportService exportService;

    public PaperController(PaperService paperService, ExportService exportService) {
        this.paperService = paperService;
        this.exportService = exportService;
    }

    @PostMapping
    public Result<PaperDTO> submit(@RequestBody PaperSubmitRequest request,
                                    Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        PaperDTO dto = paperService.submit(request, userId);
        return Result.success(dto);
    }

    @GetMapping
    public Result<Page<PaperDTO>> list(
            @RequestParam(required = false) String journalLevel,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst().map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .orElse("STUDENT");

        Page<PaperDTO> result = paperService.list(journalLevel, status, page, size, userId, role);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<PaperDTO> detail(@PathVariable Long id) {
        PaperDTO dto = paperService.detail(id);
        return Result.success(dto);
    }

    @PutMapping("/{id}")
    public Result<PaperDTO> update(@PathVariable Long id,
                                    @RequestBody PaperSubmitRequest request,
                                    Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        PaperDTO dto = paperService.update(id, request, userId);
        return Result.success(dto);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        paperService.delete(id, userId);
        return Result.success(null);
    }

    @GetMapping("/pending")
    public Result<Page<PaperDTO>> pending(
            @RequestParam(defaultValue = "SECRETARY") String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PaperDTO> result = paperService.listPending(role, page, size);
        return Result.success(result);
    }

    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) String journalLevel,
            @RequestParam(required = false) String status,
            Authentication authentication,
            HttpServletResponse response) throws Exception {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst().map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .orElse("STUDENT");
        byte[] data = exportService.exportPapers(journalLevel, status, userId, role);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode("学术论文.xlsx", StandardCharsets.UTF_8));
        try (OutputStream os = response.getOutputStream()) {
            os.write(data);
            os.flush();
        }
    }
}
