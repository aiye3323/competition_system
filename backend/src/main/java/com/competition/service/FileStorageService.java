package com.competition.service;

import com.competition.dto.FileListDTO;
import com.competition.entity.FileEntity;
import com.competition.entity.User;
import com.competition.repository.FileRepository;
import com.competition.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${app.upload.path}")
    private String uploadPath;

    private final FileRepository fileRepository;
    private final UserRepository userRepository;

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

    /**
     * 删除文件（带权限校验）
     * @param fileId 文件ID
     * @param userId 当前用户ID
     * @param role   当前用户角色
     * @param achievementStatus 关联成果的状态（可为null）
     */
    @Transactional
    public void deleteFile(Long fileId, Long userId, String role, String achievementStatus) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("文件不存在"));

        // 权限校验
        if (!canDelete(fileEntity, userId, role, achievementStatus)) {
            throw new RuntimeException("无权删除该文件");
        }

        // 软删除
        fileEntity.setIsDeleted(1);
        fileRepository.save(fileEntity);
    }

    /**
     * 判断当前用户是否有权删除文件
     */
    private boolean canDelete(FileEntity file, Long userId, String role, String achievementStatus) {
        // 管理员可删除任何文件
        if ("ROLE_ADMIN".equals(role) || "ADMIN".equals(role)) {
            return true;
        }
        // 秘书和领导不可删除
        if ("ROLE_SECRETARY".equals(role) || "SECRETARY".equals(role)
                || "ROLE_LEADER".equals(role) || "LEADER".equals(role)) {
            return false;
        }
        // 学生/教师只能删除自己上传的且成果状态为 DRAFT/REJECTED 的文件
        if (file.getUploaderId() == null || !file.getUploaderId().equals(userId)) {
            return false;
        }
        // 未关联成果的文件允许删除（草稿阶段的临时文件）
        if (achievementStatus == null) {
            return true;
        }
        // 关联了成果的文件，只有草稿或已退回状态才能删除
        return "DRAFT".equals(achievementStatus) || "REJECTED".equals(achievementStatus);
    }

    @Transactional
    public void deleteFile(Long fileId) {
        // 旧接口保留兼容：不做权限检查直接物理删除
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

    /**
     * 批量删除文件（带权限校验）
     */
    @Transactional
    public int deleteFiles(List<Long> fileIds, Long userId, String role) {
        int deleted = 0;
        for (Long id : fileIds) {
            try {
                deleteFile(id, userId, role, null);
                deleted++;
            } catch (RuntimeException ignored) {
                // 跳过无权限的
            }
        }
        return deleted;
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
     * 分页查询文件列表，支持多条件筛选和角色过滤
     */
    public Page<FileListDTO> listFiles(String fileType, String materialType, String relatedType,
                                        String keyword, LocalDate startDate, LocalDate endDate,
                                        int page, int size, Long userId, String role) {
        Specification<FileEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), 0));

            if (fileType != null && !fileType.isEmpty())
                predicates.add(cb.equal(root.get("fileType"), fileType));
            if (materialType != null && !materialType.isEmpty())
                predicates.add(cb.equal(root.get("materialType"), materialType));
            if (relatedType != null && !relatedType.isEmpty())
                predicates.add(cb.equal(root.get("relatedType"), relatedType));
            if (keyword != null && !keyword.isEmpty())
                predicates.add(cb.like(root.get("originalName"), "%" + keyword + "%"));
            if (startDate != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("uploadTime"), startDate.atStartOfDay()));
            if (endDate != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("uploadTime"), endDate.plusDays(1).atStartOfDay()));

            if ("STUDENT".equals(role) || "TEACHER".equals(role))
                predicates.add(cb.equal(root.get("uploaderId"), userId));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "uploadTime"));
        Page<FileEntity> entityPage = fileRepository.findAll(spec, pageRequest);

        Set<Long> uploaderIds = entityPage.getContent().stream()
                .map(FileEntity::getUploaderId)
                .collect(Collectors.toSet());
        Map<Long, String> nameMap = new HashMap<>();
        if (!uploaderIds.isEmpty()) {
            List<User> users = userRepository.findAllById(uploaderIds);
            for (User u : users) {
                nameMap.put(u.getId(), u.getRealName() != null ? u.getRealName() : u.getUsername());
            }
        }

        return entityPage.map(entity -> {
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
        });
    }

    /**
     * 获取文件统计：总文件数、总存储量、近7日上传数、按类型分组计数
     */
    public Map<String, Object> getFileStats(Long userId, String role) {
        Specification<FileEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), 0));
            if ("STUDENT".equals(role) || "TEACHER".equals(role))
                predicates.add(cb.equal(root.get("uploaderId"), userId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<FileEntity> allFiles = fileRepository.findAll(spec);

        long totalFiles = allFiles.size();
        long totalStorage = allFiles.stream().mapToLong(FileEntity::getFileSize).sum();
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long recentUploads = allFiles.stream()
                .filter(f -> f.getUploadTime() != null && f.getUploadTime().isAfter(sevenDaysAgo))
                .count();

        Map<String, Long> countByType = allFiles.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getFileType() != null ? f.getFileType() : "OTHER",
                        Collectors.counting()));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalFiles", totalFiles);
        stats.put("totalStorage", totalStorage);
        stats.put("recentUploads", recentUploads);
        stats.put("countByType", countByType);
        return stats;
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
     * 查询当前用户的文件列表，支持按类型筛选
     */
    public List<FileEntity> listMyFiles(Long userId, String fileType, String relatedType, String sortBy) {
        List<FileEntity> files;
        if (fileType != null && !fileType.isEmpty() && relatedType != null && !relatedType.isEmpty()) {
            files = fileRepository.findActiveByUploaderId(userId);
            files = files.stream()
                    .filter(f -> fileType.equals(f.getFileType()) && relatedType.equals(f.getRelatedType()))
                    .collect(Collectors.toList());
        } else if (fileType != null && !fileType.isEmpty()) {
            files = fileRepository.findActiveByUploaderIdAndFileType(userId, fileType);
        } else if (relatedType != null && !relatedType.isEmpty()) {
            files = fileRepository.findActiveByUploaderIdAndRelatedType(userId, relatedType);
        } else {
            files = fileRepository.findActiveByUploaderId(userId);
        }

        // 排序
        if ("name".equals(sortBy)) {
            files.sort(Comparator.comparing(FileEntity::getOriginalName, String.CASE_INSENSITIVE_ORDER));
        } else if ("size".equals(sortBy)) {
            files.sort(Comparator.comparingLong(FileEntity::getFileSize));
        } else {
            files.sort(Comparator.comparing(FileEntity::getUploadTime).reversed());
        }
        return files;
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
