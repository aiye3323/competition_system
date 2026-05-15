package com.competition.controller;

import com.competition.dto.FileListDTO;
import com.competition.dto.Result;
import com.competition.entity.FileEntity;
import com.competition.entity.User;
import com.competition.repository.FileRepository;
import com.competition.repository.UserRepository;
import com.competition.service.FileExportService;
import com.competition.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
public class FileController {

    // 1. 保留 final 声明
    private final FileStorageService fileStorageService;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FileExportService fileExportService;

    // 2. 手动写构造器 → 彻底解决”未初始化”报错
    public FileController(FileStorageService fileStorageService, FileRepository fileRepository,
                          UserRepository userRepository, FileExportService fileExportService) {
        this.fileStorageService = fileStorageService;
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.fileExportService = fileExportService;
    }

    /**
     * 检查当前用户是否有权访问该文件
     * ADMIN/SECRETARY/LEADER 可访问所有文件
     * STUDENT/TEACHER 只能访问自己上传的文件
     */
    private boolean canAccessFile(FileEntity file, Authentication auth) {
        if (auth == null) return false;
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst().map(Object::toString).orElse("");
        // 管理员、秘书、领导可访问所有
        if (role.contains("ADMIN") || role.contains("SECRETARY") || role.contains("LEADER")) {
            return true;
        }
        // 学生/教师只能访问自己上传的文件
        return file.getUploaderId() != null && file.getUploaderId().equals(userId);
    }

    private String getUserRole(Authentication auth) {
        if (auth == null) return "";
        return auth.getAuthorities().stream()
                .findFirst().map(Object::toString).orElse("");
    }

    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file,
                                                  @RequestParam(value = "materialType", required = false) String materialType,
                                                  Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        FileEntity fileEntity = fileStorageService.storeFile(file, userId, materialType);

        Map<String, Object> data = new HashMap<>();
        data.put("id", fileEntity.getId());
        data.put("originalName", fileEntity.getOriginalName());
        data.put("fileSize", fileEntity.getFileSize());
        data.put("fileType", fileEntity.getFileType());
        data.put("materialType", fileEntity.getMaterialType());

        return Result.success(data);
    }

    @PostMapping("/upload/batch")
    public Result<List<Map<String, Object>>> uploadFiles(@RequestParam("files") List<MultipartFile> files,
                                                         @RequestParam(value = "materialType", required = false) String materialType,
                                                         Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<Map<String, Object>> resultList = new ArrayList<>();

        for (MultipartFile file : files) {
            FileEntity fileEntity = fileStorageService.storeFile(file, userId, materialType);
            Map<String, Object> data = new HashMap<>();
            data.put("id", fileEntity.getId());
            data.put("originalName", fileEntity.getOriginalName());
            data.put("fileSize", fileEntity.getFileSize());
            data.put("fileType", fileEntity.getFileType());
            data.put("materialType", fileEntity.getMaterialType());
            resultList.add(data);
        }

        return Result.success(resultList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFile(@PathVariable Long id) {
        FileEntity fileEntity = fileRepository.findById(id).orElse(null);
        if (fileEntity == null) {
            return ResponseEntity.status(404).body(Result.error(404, "文件记录不存在"));
        }

        // inline 预览不检查权限：浏览器 <img>/<iframe> 不发送 Authorization header
        // 下载端点 /{id}/download 有权限校验

        Resource resource;
        try {
            resource = fileStorageService.loadFileAsResource(fileEntity.getStoragePath());
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Result.error(400, e.getMessage()));
        }

        String contentType = fileStorageService.getContentType(fileEntity.getStoragePath());
        String safeName = fileEntity.getOriginalName().replaceAll("[\"\\n\\r]", "_");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\""
                                + safeName
                                + "\"; filename*=UTF-8''"
                                + encodeRFC5987(safeName))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(resource);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadFile(@PathVariable Long id,
                                          @RequestParam(required = false) String filename,
                                          Authentication authentication) {
        FileEntity fileEntity = fileRepository.findById(id).orElse(null);
        if (fileEntity == null) {
            return ResponseEntity.status(404).body(Result.error(404, "文件记录不存在"));
        }

        if (!canAccessFile(fileEntity, authentication)) {
            return ResponseEntity.status(403).body(Result.error(403, "无权下载该文件"));
        }

        Resource resource;
        try {
            resource = fileStorageService.loadFileAsResource(fileEntity.getStoragePath());
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Result.error(400, e.getMessage()));
        }

        String contentType = fileStorageService.getContentType(fileEntity.getStoragePath());

        // 文件名优先级：前端传入 > materialType + ext > originalName
        String displayName;
        if (filename != null) {
            displayName = filename;
        } else if (fileEntity.getMaterialType() != null && !fileEntity.getMaterialType().isEmpty()) {
            String ext = "";
            String orig = fileEntity.getOriginalName();
            if (orig != null && orig.contains(".")) {
                ext = orig.substring(orig.lastIndexOf("."));
            }
            displayName = fileEntity.getMaterialType() + ext;
        } else {
            displayName = fileEntity.getOriginalName();
        }
        displayName = displayName.replaceAll("[\"\\n\\r]", "_");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + displayName
                                + "\"; filename*=UTF-8''"
                                + encodeRFC5987(displayName))
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteFile(@PathVariable Long id,
                                   @RequestParam(required = false) String achievementStatus,
                                   Authentication authentication) {
        if (authentication == null)
            return Result.error(401, "请先登录");

        Long userId = (Long) authentication.getPrincipal();
        String role = getUserRole(authentication);

        // 检查是否为删除者上传的文件
        FileEntity fileEntity = fileRepository.findById(id).orElse(null);
        if (fileEntity == null)
            return Result.error(404, "文件不存在");

        // 管理员可删除任何文件（二次确认由前端处理）
        if (role.contains("ADMIN")) {
            fileStorageService.deleteFile(id);
            return Result.success(null);
        }

        // 秘书/领导不可删除
        if (role.contains("SECRETARY") || role.contains("LEADER")) {
            return Result.error(403, "审核人员无权删除文件");
        }

        // 学生/教师：只能删除自己的文件且成果状态为 DRAFT 或 REJECTED 或未关联
        if (!fileEntity.getUploaderId().equals(userId)) {
            return Result.error(403, "无权删除他人文件");
        }

        fileStorageService.deleteFile(id, userId, role, achievementStatus);
        return Result.success(null);
    }

    /**
     * 批量删除文件
     */
    @DeleteMapping("/batch")
    public Result<Map<String, Object>> batchDeleteFiles(@RequestBody Map<String, Object> body,
                                                         Authentication authentication) {
        if (authentication == null)
            return Result.error(401, "请先登录");

        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) body.get("fileIds");
        if (rawIds == null || rawIds.isEmpty())
            return Result.error(400, "未指定要删除的文件");

        List<Long> fileIds = rawIds.stream().map(Long::valueOf).toList();
        Long userId = (Long) authentication.getPrincipal();
        String role = getUserRole(authentication);

        int deleted = fileStorageService.deleteFiles(fileIds, userId, role);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deleted", deleted);
        result.put("total", fileIds.size());
        return Result.success(result);
    }

    /**
     * 分页查询文件列表，支持多条件筛选
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> listFiles(
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String materialType,
            @RequestParam(required = false) String relatedType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        if (authentication == null)
            return Result.error(401, "请先登录");

        Long userId = (Long) authentication.getPrincipal();
        String role = getUserRole(authentication);

        Page<FileListDTO> dtoPage = fileStorageService.listFiles(
                fileType, materialType, relatedType, keyword,
                startDate, endDate, page, size, userId, role);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", dtoPage.getContent());
        result.put("totalElements", dtoPage.getTotalElements());
        result.put("totalPages", dtoPage.getTotalPages());
        result.put("number", dtoPage.getNumber());
        result.put("size", dtoPage.getSize());
        return Result.success(result);
    }

    /**
     * 当前用户个人文件列表
     */
    @GetMapping("/my")
    public Result<List<FileListDTO>> getMyFiles(
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String relatedType,
            @RequestParam(required = false, defaultValue = "time") String sortBy,
            Authentication authentication) {

        if (authentication == null)
            return Result.error(401, "请先登录");

        Long userId = (Long) authentication.getPrincipal();

        List<FileEntity> files = fileStorageService.listMyFiles(userId, fileType, relatedType, sortBy);

        Set<Long> uploaderIds = files.stream()
                .map(FileEntity::getUploaderId)
                .collect(Collectors.toSet());
        Map<Long, String> nameMap = new HashMap<>();
        if (!uploaderIds.isEmpty()) {
            List<User> users = userRepository.findAllById(uploaderIds);
            for (User u : users) {
                nameMap.put(u.getId(), u.getRealName() != null ? u.getRealName() : u.getUsername());
            }
        }

        List<FileListDTO> dtos = files.stream().map(entity -> {
            FileListDTO dto = new FileListDTO();
            dto.setId(entity.getId());
            dto.setOriginalName(entity.getOriginalName());
            dto.setStoragePath(entity.getStoragePath());
            dto.setFileSize(entity.getFileSize());
            dto.setFileType(entity.getFileType());
            dto.setMaterialType(entity.getMaterialType());
            dto.setRelatedType(entity.getRelatedType());
            dto.setRelatedId(entity.getRelatedId());
            dto.setUploaderId(entity.getUploaderId());
            dto.setUploaderName(nameMap.getOrDefault(entity.getUploaderId(), "未知"));
            dto.setUploadTime(entity.getUploadTime());
            return dto;
        }).collect(Collectors.toList());

        return Result.success(dtos);
    }

    /**
     * 获取文件统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getFileStats(Authentication authentication) {
        if (authentication == null)
            return Result.error(401, "请先登录");

        Long userId = (Long) authentication.getPrincipal();
        String role = getUserRole(authentication);

        Map<String, Object> stats = fileStorageService.getFileStats(userId, role);
        return Result.success(stats);
    }

    /**
     * 打包下载单个成果的全部证明材料
     */
    @GetMapping("/download-all/{relatedType}/{relatedId}")
    public ResponseEntity<?> downloadAllFiles(@PathVariable String relatedType,
                                               @PathVariable Long relatedId) {
        List<FileEntity> files = fileRepository.findByRelatedTypeAndRelatedId(relatedType, relatedId);

        if (files == null || files.isEmpty()) {
            return ResponseEntity.status(404).body(Result.error(404, "该成果没有证明材料"));
        }

        byte[] zipBytes = fileStorageService.zipFiles(files);

        String zipName = relatedType + "_" + relatedId + "_全部材料.zip";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + zipName.replaceAll("[\"\\n\\r]", "_")
                                + "\"; filename*=UTF-8''"
                                + encodeRFC5987(zipName))
                .body(zipBytes);
    }

    /**
     * 打包下载选中的文件（通过文件 ID 列表）
     * 支持传入成果信息，用于生成有意义的 ZIP 文件名
     */
    @PostMapping("/download-selected")
    public ResponseEntity<?> downloadSelectedFiles(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) body.get("fileIds");
        if (rawIds == null || rawIds.isEmpty()) {
            return ResponseEntity.status(400).body(Result.error(400, "未选择文件"));
        }
        List<Long> fileIds = rawIds.stream().map(Long::valueOf).toList();

        List<FileEntity> files = fileRepository.findAllById(fileIds);
        if (files.isEmpty()) {
            return ResponseEntity.status(404).body(Result.error(404, "文件不存在"));
        }

        byte[] zipBytes = fileStorageService.zipFiles(files);

        // 动态生成 ZIP 文件名
        String typeName = (String) body.getOrDefault("typeName", "");
        String applicantName = (String) body.getOrDefault("applicantName", "");
        String achievementName = (String) body.getOrDefault("achievementName", "");

        String zipName;
        if (!typeName.isEmpty() && !applicantName.isEmpty() && !achievementName.isEmpty()) {
            // 单成果：项目_张三_预览测试_选中材料.zip
            String safeAchievement = achievementName.length() > 20
                    ? achievementName.substring(0, 20) : achievementName;
            zipName = typeName + "_" + applicantName + "_" + safeAchievement + "_选中材料.zip";
            zipName = zipName.replaceAll("[\\\\/:*?\"<>|\\s]", "");
        } else {
            // 批量跨成果：批量成果_操作人_日期.zip
            String operator = !applicantName.isEmpty() ? applicantName : "用户";
            String date = java.time.LocalDate.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            zipName = "批量成果_" + operator + "_" + date + ".zip";
            zipName = zipName.replaceAll("[\\\\/:*?\"<>|\\s]", "");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + zipName
                                + "\"; filename*=UTF-8''"
                                + encodeRFC5987(zipName))
                .body(zipBytes);
    }

    /**
     * 一键导出所有文件（按成果分类打包 ZIP）
     */
    @GetMapping("/export-all")
    public ResponseEntity<?> exportAllFiles(Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).body(Result.error(401, "请先登录"));

        Long userId = (Long) authentication.getPrincipal();
        String role = getUserRole(authentication);

        byte[] zipBytes = fileExportService.exportAllFiles(userId, role);

        String zipName = "科研成果文件导出_"
                + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                + ".zip";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + zipName
                                + "\"; filename*=UTF-8''"
                                + encodeRFC5987(zipName))
                .body(zipBytes);
    }

    /**
     * RFC 5987 文件名编码（支持中文）
     */
    private String encodeRFC5987(String filename) {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
    }
}