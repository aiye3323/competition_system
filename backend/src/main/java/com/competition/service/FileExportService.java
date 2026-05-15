package com.competition.service;

import com.competition.entity.*;
import com.competition.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;

@Service
@RequiredArgsConstructor
public class FileExportService {

    @Value("${app.upload.path}")
    private String uploadPath;

    private final FileRepository fileRepository;
    private final CompetitionRepository competitionRepository;
    private final ProjectRepository projectRepository;
    private final PaperRepository paperRepository;
    private final SoftwareRepository softwareRepository;
    private final UserRepository userRepository;

    private Path baseUploadPath;

    private static final Map<String, String> TYPE_LABEL = Map.of(
        "COMPETITION", "学科竞赛",
        "PROJECT", "创新项目",
        "PAPER", "学术论文",
        "SOFTWARE", "软件著作"
    );

    @PostConstruct
    public void init() {
        this.baseUploadPath = Paths.get(uploadPath).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public byte[] exportAllFiles(Long userId, String role) {
        // 1. 收集所有已归档成果及其文件
        Map<Long, String> userNameMap = new HashMap<>();
        List<ExportEntry> entries = new ArrayList<>();

        collectCompetitions(entries, userNameMap);
        collectProjects(entries, userNameMap);
        collectPapers(entries, userNameMap);
        collectSoftwares(entries, userNameMap);

        // 2. 角色过滤：学生/教师只看自己的
        if (role.contains("STUDENT") || role.contains("TEACHER")) {
            entries = entries.stream()
                .filter(e -> e.uploaderId != null && e.uploaderId.equals(userId))
                .toList();
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("export_");

            int fileCount = copyEntriesToStructure(entries, tempDir);
            writeMetadata(tempDir, fileCount, entries.size());

            return zipDirectory(tempDir);
        } catch (IOException e) {
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        } finally {
            if (tempDir != null) {
                try { deleteDirectory(tempDir); } catch (IOException ignored) {}
            }
        }
    }

    // ========== Data model ==========

    private static class ExportEntry {
        String typeLabel;       // 学科竞赛 / 创新项目 / ...
        String personName;      // 申请人姓名
        String achievementName; // 成果名称
        String materialLabel;   // 材料类型（获奖证书 / 立项申报书 / ...）
        String storagePath;     // 文件存储路径
        String originalName;    // 原始文件名（兜底）
        Long uploaderId;        // 上传者 ID
    }

    // ========== Collect archived achievements ==========

    private void collectCompetitions(List<ExportEntry> entries, Map<Long, String> userNameMap) {
        List<Competition> list = competitionRepository.findByStatusAndIsDeleted("ARCHIVED", 0);
        for (Competition c : list) {
            String personName = resolveUserName(c.getApplicant(), userNameMap);
            String achName = sanitize(c.getCompetitionName());

            // a. 实体直存路径（旧版上传）
            addEntryIfValid(entries, c.getCertificatePath(), "获奖证书", "COMPETITION",
                personName, achName, c.getApplicant() != null ? c.getApplicant().getId() : null);

            // b. FileEntity 关联（新版上传）
            List<FileEntity> files = fileRepository.findByRelatedTypeAndRelatedId("COMPETITION", c.getId());
            for (FileEntity f : files) {
                if (f.getIsDeleted() != null && f.getIsDeleted() == 1) continue;
                entries.add(buildEntryFromFile(f, "COMPETITION", personName, achName));
            }
        }
    }

    private void collectProjects(List<ExportEntry> entries, Map<Long, String> userNameMap) {
        List<Project> list = projectRepository.findByStatusAndIsDeleted("ARCHIVED", 0);
        for (Project p : list) {
            String personName = resolveUserName(p.getApplicant(), userNameMap);
            String achName = sanitize(p.getProjectName());

            addEntryIfValid(entries, p.getProposalPath(), "立项申报书", "PROJECT",
                personName, achName, p.getApplicant() != null ? p.getApplicant().getId() : null);
            addEntryIfValid(entries, p.getConclusionPath(), "结题材料", "PROJECT",
                personName, achName, p.getApplicant() != null ? p.getApplicant().getId() : null);

            List<FileEntity> files = fileRepository.findByRelatedTypeAndRelatedId("PROJECT", p.getId());
            for (FileEntity f : files) {
                if (f.getIsDeleted() != null && f.getIsDeleted() == 1) continue;
                entries.add(buildEntryFromFile(f, "PROJECT", personName, achName));
            }
        }
    }

    private void collectPapers(List<ExportEntry> entries, Map<Long, String> userNameMap) {
        List<Paper> list = paperRepository.findByStatusAndIsDeleted("ARCHIVED", 0);
        for (Paper p : list) {
            String personName = resolveUserName(p.getApplicant(), userNameMap);
            String achName = sanitize(p.getTitle());

            addEntryIfValid(entries, p.getDraftPath(), "投稿初稿", "PAPER",
                personName, achName, p.getApplicant() != null ? p.getApplicant().getId() : null);

            List<FileEntity> files = fileRepository.findByRelatedTypeAndRelatedId("PAPER", p.getId());
            for (FileEntity f : files) {
                if (f.getIsDeleted() != null && f.getIsDeleted() == 1) continue;
                entries.add(buildEntryFromFile(f, "PAPER", personName, achName));
            }
        }
    }

    private void collectSoftwares(List<ExportEntry> entries, Map<Long, String> userNameMap) {
        List<Software> list = softwareRepository.findByStatusAndIsDeleted("ARCHIVED", 0);
        for (Software s : list) {
            String personName = resolveUserName(s.getApplicant(), userNameMap);
            String achName = sanitize(s.getSoftwareName());

            addEntryIfValid(entries, s.getMaterialPath(), "申报材料", "SOFTWARE",
                personName, achName, s.getApplicant() != null ? s.getApplicant().getId() : null);
            addEntryIfValid(entries, s.getCertificatePath(), "证书扫描件", "SOFTWARE",
                personName, achName, s.getApplicant() != null ? s.getApplicant().getId() : null);

            List<FileEntity> files = fileRepository.findByRelatedTypeAndRelatedId("SOFTWARE", s.getId());
            for (FileEntity f : files) {
                if (f.getIsDeleted() != null && f.getIsDeleted() == 1) continue;
                entries.add(buildEntryFromFile(f, "SOFTWARE", personName, achName));
            }
        }
    }

    // ========== Helpers ==========

    private String resolveUserName(User applicant, Map<Long, String> userNameMap) {
        if (applicant == null) return "未知用户";
        Long id = applicant.getId();
        if (!userNameMap.containsKey(id)) {
            String name = applicant.getRealName() != null ? applicant.getRealName() : applicant.getUsername();
            userNameMap.put(id, name != null ? name : "未知用户");
        }
        return userNameMap.get(id);
    }

    private void addEntryIfValid(List<ExportEntry> entries, String path, String materialLabel,
                                  String type, String personName, String achName, Long uploaderId) {
        if (path == null || path.isEmpty()) return;
        ExportEntry e = new ExportEntry();
        e.typeLabel = TYPE_LABEL.getOrDefault(type, type);
        e.personName = personName;
        e.achievementName = achName;
        e.materialLabel = materialLabel;
        e.storagePath = path;
        e.originalName = extractFileName(path);
        e.uploaderId = uploaderId;
        entries.add(e);
    }

    private ExportEntry buildEntryFromFile(FileEntity f, String type, String personName, String achName) {
        ExportEntry e = new ExportEntry();
        e.typeLabel = TYPE_LABEL.getOrDefault(type, type);
        e.personName = personName;
        e.achievementName = achName;
        e.materialLabel = f.getMaterialType();
        e.storagePath = f.getStoragePath();
        e.originalName = f.getOriginalName();
        e.uploaderId = f.getUploaderId();
        return e;
    }

    private String extractFileName(String path) {
        if (path == null) return "unknown";
        String name = path.replace('\\', '/');
        int idx = name.lastIndexOf('/');
        return idx >= 0 ? name.substring(idx + 1) : name;
    }

    // ========== File copying ==========

    private int copyEntriesToStructure(List<ExportEntry> entries, Path tempDir) throws IOException {
        int count = 0;
        Set<String> usedPaths = new HashSet<>();

        for (ExportEntry e : entries) {
            Path srcPath = resolveSourcePath(e.storagePath);
            if (srcPath == null) continue;

            String dirName = e.personName + "_" + e.achievementName;

            // 文件名：材料类型 + 扩展名
            String fileName;
            if (e.materialLabel != null && !e.materialLabel.isEmpty()) {
                String ext = "";
                String orig = e.originalName != null ? e.originalName : "";
                if (orig.contains(".")) {
                    ext = orig.substring(orig.lastIndexOf("."));
                }
                fileName = e.materialLabel + ext;
            } else {
                fileName = e.originalName != null ? e.originalName : "unknown";
            }
            fileName = sanitizeFilename(fileName);

            String relativePath = e.typeLabel + "/" + dirName + "/" + fileName;

            // Dedup
            int dup = 1;
            String candidate = relativePath;
            while (usedPaths.contains(candidate)) {
                int dot = fileName.lastIndexOf('.');
                String base = dot >= 0 ? fileName.substring(0, dot) : fileName;
                String ext = dot >= 0 ? fileName.substring(dot) : "";
                candidate = e.typeLabel + "/" + dirName + "/" + base + "(" + dup + ")" + ext;
                dup++;
            }
            usedPaths.add(candidate);

            Path destFile = tempDir.resolve(candidate);
            Files.createDirectories(destFile.getParent());
            Files.copy(srcPath, destFile, StandardCopyOption.REPLACE_EXISTING);
            count++;
        }

        return count;
    }

    /**
     * 解析文件路径：先尝试 baseUploadPath 下的相对路径，再尝试绝对路径
     */
    private Path resolveSourcePath(String storagePath) {
        if (storagePath == null || storagePath.isEmpty()) return null;

        // 尝试1: 相对于 upload 目录
        Path relative = baseUploadPath.resolve(storagePath).normalize();
        if (relative.startsWith(baseUploadPath) && Files.exists(relative) && Files.isReadable(relative)) {
            return relative;
        }

        // 尝试2: 绝对路径
        Path absolute = Paths.get(storagePath).normalize();
        if (Files.exists(absolute) && Files.isReadable(absolute)) {
            return absolute;
        }

        // 尝试3: storagePath 可能已包含 upload 目录前缀
        // 例如 storagePath = "uploads/2026/05/14/xxx.png" 但 baseUploadPath = "./uploads"
        // 这种情况下 baseUploadPath.resolve 会正确解析，但上面已经试过了
        return null;
    }

    // ========== Metadata ==========

    private void writeMetadata(Path tempDir, int copiedCount, int totalEntries) throws IOException {
        String json = String.format(
            "{\"exportTime\":\"%s\",\"copiedFiles\":%d,\"totalEntries\":%d,\"version\":\"1.0\"}",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            copiedCount, totalEntries
        );
        Files.writeString(tempDir.resolve("export_metadata.json"), json, StandardCharsets.UTF_8);
    }

    // ========== ZIP ==========

    private byte[] zipDirectory(Path sourceDir) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            try (var stream = Files.walk(sourceDir)) {
                stream.filter(Files::isRegularFile).forEach(file -> {
                    try {
                        String entryName = sourceDir.relativize(file).toString()
                            .replace('\\', '/');
                        zos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(file, zos);
                        zos.closeEntry();
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                });
            }

            zos.finish();
            return baos.toByteArray();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    // ========== Utility ==========

    private String sanitize(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[\\\\/:*?\"<>|\\n\\r]", "_").trim();
    }

    private String sanitizeFilename(String name) {
        if (name == null || name.isEmpty()) return "unknown";
        return name.replaceAll("[\\\\/:*?\"<>|\\n\\r]", "_");
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); }
                        catch (IOException ignored) {}
                    });
            }
        }
    }
}
