package com.competition.controller;

import com.competition.dto.Result;
import com.competition.entity.FileEntity;
import com.competition.repository.FileRepository;
import com.competition.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    // 1. 保留 final 声明
    private final FileStorageService fileStorageService;
    private final FileRepository fileRepository;

    // 2. 手动写构造器 → 彻底解决“未初始化”报错
    public FileController(FileStorageService fileStorageService, FileRepository fileRepository) {
        this.fileStorageService = fileStorageService;
        this.fileRepository = fileRepository;
    }

    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file,
                                                  Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        FileEntity fileEntity = fileStorageService.storeFile(file, userId);

        Map<String, Object> data = new HashMap<>();
        data.put("id", fileEntity.getId());
        data.put("originalName", fileEntity.getOriginalName());
        data.put("fileSize", fileEntity.getFileSize());
        data.put("fileType", fileEntity.getFileType());

        return Result.success(data);
    }

    @PostMapping("/upload/batch")
    public Result<List<Map<String, Object>>> uploadFiles(@RequestParam("files") List<MultipartFile> files,
                                                         Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<Map<String, Object>> resultList = new ArrayList<>();

        for (MultipartFile file : files) {
            FileEntity fileEntity = fileStorageService.storeFile(file, userId);
            Map<String, Object> data = new HashMap<>();
            data.put("id", fileEntity.getId());
            data.put("originalName", fileEntity.getOriginalName());
            data.put("fileSize", fileEntity.getFileSize());
            data.put("fileType", fileEntity.getFileType());
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
                        "inline; filename=\"" + safeName + "\"")
                .body(resource);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadFile(@PathVariable Long id,
                                          @RequestParam(required = false) String filename) {
        FileEntity fileEntity = fileRepository.findById(id).orElse(null);
        if (fileEntity == null) {
            return ResponseEntity.status(404).body(Result.error(404, "文件记录不存在"));
        }

        Resource resource;
        try {
            resource = fileStorageService.loadFileAsResource(fileEntity.getStoragePath());
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Result.error(400, e.getMessage()));
        }

        String contentType = fileStorageService.getContentType(fileEntity.getStoragePath());
        String displayName = filename != null ? filename : fileEntity.getOriginalName();
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
    public Result<Void> deleteFile(@PathVariable Long id) {
        fileStorageService.deleteFile(id);
        return Result.success(null);
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
     */
    @PostMapping("/download-selected")
    public ResponseEntity<?> downloadSelectedFiles(@RequestBody Map<String, List<Long>> body) {
        List<Long> fileIds = body.get("fileIds");
        if (fileIds == null || fileIds.isEmpty()) {
            return ResponseEntity.status(400).body(Result.error(400, "未选择文件"));
        }

        List<FileEntity> files = fileRepository.findAllById(fileIds);
        if (files.isEmpty()) {
            return ResponseEntity.status(404).body(Result.error(404, "文件不存在"));
        }

        byte[] zipBytes = fileStorageService.zipFiles(files);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"selected_materials.zip\"")
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