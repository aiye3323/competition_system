package com.competition.service;

import com.competition.entity.FileEntity;
import com.competition.repository.FileRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${app.upload.path}")
    private String uploadPath;

    private final FileRepository fileRepository;

    /** 基础上传目录的绝对路径（启动时初始化，避免每次拼路径） */
    private Path baseUploadPath;

    @PostConstruct
    public void init() {
        try {
            this.baseUploadPath = Paths.get(uploadPath).toAbsolutePath().normalize();
            Files.createDirectories(baseUploadPath);
        } catch (IOException e) {
            throw new RuntimeException("无法创建上传目录: " + uploadPath, e);
        }
    }

    @Transactional
    public FileEntity storeFile(MultipartFile file, Long uploaderId) {
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }

        // 按日期分目录: ./uploads/2026/05/14/uuid.ext
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuidName = UUID.randomUUID().toString() + extension;
        String relativePath = dateDir + "/" + uuidName;

        try {
            Path targetDir = baseUploadPath.resolve(dateDir).normalize();
            Files.createDirectories(targetDir);
            Path targetPath = baseUploadPath.resolve(relativePath).normalize();
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("文件存储失败", e);
        }

        String fileType = detectFileType(extension);

        FileEntity fileEntity = new FileEntity();
        fileEntity.setOriginalName(originalName != null ? originalName : "unknown");
        fileEntity.setStoragePath(relativePath);
        fileEntity.setFileSize(file.getSize());
        fileEntity.setFileType(fileType);
        fileEntity.setUploaderId(uploaderId);
        fileEntity.setUploadTime(LocalDateTime.now());

        return fileRepository.save(fileEntity);
    }

    public Resource loadFileAsResource(String storagePath) {
        Path filePath = baseUploadPath.resolve(storagePath).normalize();

        // 安全检查：防止路径遍历攻击
        if (!filePath.startsWith(baseUploadPath)) {
            throw new RuntimeException("非法的文件路径");
        }

        // 使用 FileSystemResource 读取，避免 URI 编码问题
        Resource resource = new FileSystemResource(filePath);
        if (resource.exists() && resource.isReadable()) {
            return resource;
        }
        throw new RuntimeException("文件不存在或不可读: " + filePath);
    }

    @Transactional
    public void deleteFile(Long fileId) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("文件不存在"));

        try {
            Path filePath = baseUploadPath.resolve(fileEntity.getStoragePath()).normalize();
            if (!filePath.startsWith(baseUploadPath)) {
                throw new RuntimeException("非法的文件路径");
            }
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("文件删除失败", e);
        }

        fileRepository.delete(fileEntity);
    }

    @Transactional
    public void linkFilesToAchievement(List<Long> fileIds, String relatedType, Long relatedId) {
        if (fileIds == null || fileIds.isEmpty()) return;
        for (Long fileId : fileIds) {
            FileEntity fileEntity = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("文件不存在: " + fileId));
            fileEntity.setRelatedType(relatedType);
            fileEntity.setRelatedId(relatedId);
            fileRepository.save(fileEntity);
        }
    }

    public String getContentType(String storagePath) {
        String name = storagePath.toLowerCase();
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".doc")) return "application/msword";
        if (name.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (name.endsWith(".zip")) return "application/zip";
        if (name.endsWith(".mp4")) return "video/mp4";
        return "application/octet-stream";
    }

    private String detectFileType(String extension) {
        String ext = extension.toLowerCase();
        if (ext.matches("\\.(png|jpg|jpeg|gif|bmp|webp)")) return "IMAGE";
        if (ext.matches("\\.(pdf|doc|docx|xls|xlsx|ppt|pptx|txt)")) return "DOCUMENT";
        if (ext.matches("\\.(zip|rar|7z|tar|gz)")) return "ARCHIVE";
        if (ext.matches("\\.(mp4|avi|mov|wmv|flv)")) return "VIDEO";
        return "OTHER";
    }
}
