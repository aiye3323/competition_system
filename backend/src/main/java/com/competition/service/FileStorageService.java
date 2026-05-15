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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
        return storeFile(file, uploaderId, null);
    }

    @Transactional
    public FileEntity storeFile(MultipartFile file, Long uploaderId, String materialType) {
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
        fileEntity.setMaterialType(materialType);
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

    /**
     * 获取原始文件名（不含路径）
     */
    public String getOriginalName(String storagePath) {
        String name = storagePath.replace('\\', '/');
        int idx = name.lastIndexOf('/');
        return idx >= 0 ? name.substring(idx + 1) : name;
    }

    /**
     * 构建下载文件名：【类型】_【申报人】_【名称】_【文件用途】.扩展名
     */
    public String buildDownloadName(String typeName, String applicantName,
                                     String achievementName, String fileLabel, String extension) {
        String safeName = (typeName + "_" + applicantName + "_" + achievementName + "_" + fileLabel)
                .replaceAll("[\\\\/:*?\"<>|]", "")
                .replaceAll("\\s+", "");
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        return safeName + extension;
    }

    /**
     * 将多个文件打包为 ZIP 字节数组（内存中完成）
     * @param entities 文件实体列表
     * @return ZIP 字节数组
     */
    public byte[] zipFiles(List<FileEntity> entities) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            Set<String> usedNames = new HashSet<>();

            for (FileEntity entity : entities) {
                Path filePath = baseUploadPath.resolve(entity.getStoragePath()).normalize();

                if (!filePath.startsWith(baseUploadPath)) {
                    continue;
                }

                if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                    continue;
                }

                String entryName = entity.getOriginalName();
                // 同名文件去重
                int dupCount = 1;
                String candidate = entryName;
                while (usedNames.contains(candidate)) {
                    int dotIdx = entryName.lastIndexOf('.');
                    String base = dotIdx >= 0 ? entryName.substring(0, dotIdx) : entryName;
                    String ext = dotIdx >= 0 ? entryName.substring(dotIdx) : "";
                    candidate = base + "(" + dupCount + ")" + ext;
                    dupCount++;
                }
                usedNames.add(candidate);

                ZipEntry zipEntry = new ZipEntry(candidate);
                zos.putNextEntry(zipEntry);

                try (InputStream is = Files.newInputStream(filePath)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }

            zos.finish();
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("ZIP 打包失败", e);
        }
    }
}
