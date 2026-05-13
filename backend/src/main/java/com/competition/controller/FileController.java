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
    public ResponseEntity<Resource> getFile(@PathVariable Long id) {
        FileEntity fileEntity = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文件不存在"));

        Resource resource = fileStorageService.loadFileAsResource(fileEntity.getStoragePath());
        String contentType = fileStorageService.getContentType(fileEntity.getStoragePath());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + fileEntity.getOriginalName() + "\"")
                .body(resource);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id,
                                                  @RequestParam(required = false) String filename) {
        FileEntity fileEntity = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文件不存在"));

        Resource resource = fileStorageService.loadFileAsResource(fileEntity.getStoragePath());
        String contentType = fileStorageService.getContentType(fileEntity.getStoragePath());

        // 支持自定义下载文件名（用于展示类别、项目、作者等信息）
        String displayName = filename != null ? filename : fileEntity.getOriginalName();
        displayName = displayName.replaceAll("[\"\\n\\r]", "_");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + displayName + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteFile(@PathVariable Long id) {
        fileStorageService.deleteFile(id);
        return Result.success(null);
    }
}