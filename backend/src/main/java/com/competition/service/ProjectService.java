package com.competition.service;

import com.competition.dto.FileInfo;
import com.competition.dto.ProjectDTO;
import com.competition.dto.ProjectSubmitRequest;
import com.competition.entity.FileEntity;
import com.competition.entity.Project;
import com.competition.entity.User;
import com.competition.repository.FileRepository;
import com.competition.repository.ProjectRepository;
import com.competition.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public ProjectDTO submit(ProjectSubmitRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Project project = new Project();
        project.setProjectName(request.getProjectName());
        project.setProjectLevel(request.getProjectLevel());
        project.setProjectType(request.getProjectType());
        project.setAdvisor(request.getAdvisor());
        project.setMembers(request.getMembers());
        project.setEstablishTime(request.getEstablishTime());
        project.setStatus("PENDING");
        project.setApplicant(user);
        project.setApplyTime(LocalDateTime.now());

        project = projectRepository.save(project);

        if (request.getFileIds() != null && !request.getFileIds().isEmpty()) {
            fileStorageService.linkFilesToAchievement(request.getFileIds(), "PROJECT", project.getId());
        }

        return toDTO(project);
    }

    public Page<ProjectDTO> list(String projectLevel, String status, int page, int size, Long userId, String role) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "applyTime"));

        Specification<Project> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), 0));

            if (projectLevel != null && !projectLevel.isEmpty()) {
                predicates.add(cb.equal(root.get("projectLevel"), projectLevel));
            }
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if ("STUDENT".equals(role) || "TEACHER".equals(role)) {
                predicates.add(cb.equal(root.get("applicant").get("id"), userId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return projectRepository.findAll(spec, pageable).map(this::toDTO);
    }

    public ProjectDTO detail(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("项目记录不存在"));
        return toDTO(project);
    }

    @Transactional
    public ProjectDTO update(Long id, ProjectSubmitRequest request, Long userId) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("项目记录不存在"));

        if (!"REJECTED".equals(project.getStatus())) {
            throw new RuntimeException("当前状态不可修改");
        }
        if (!project.getApplicant().getId().equals(userId)) {
            throw new RuntimeException("只能修改自己的记录");
        }

        project.setProjectName(request.getProjectName());
        project.setProjectLevel(request.getProjectLevel());
        project.setProjectType(request.getProjectType());
        project.setAdvisor(request.getAdvisor());
        project.setMembers(request.getMembers());
        project.setEstablishTime(request.getEstablishTime());
        project.setStatus("PENDING");

        project = projectRepository.save(project);

        List<FileEntity> oldFiles = fileRepository.findByRelatedTypeAndRelatedId("PROJECT", id);
        for (FileEntity f : oldFiles) {
            f.setRelatedType(null);
            f.setRelatedId(null);
            fileRepository.save(f);
        }
        if (request.getFileIds() != null && !request.getFileIds().isEmpty()) {
            fileStorageService.linkFilesToAchievement(request.getFileIds(), "PROJECT", project.getId());
        }

        return toDTO(project);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("项目记录不存在"));

        if (!project.getApplicant().getId().equals(userId)) {
            throw new RuntimeException("只能删除自己的记录");
        }

        project.setIsDeleted(1);
        projectRepository.save(project);
    }

    public Page<ProjectDTO> listPending(String role, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "applyTime"));

        Specification<Project> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), 0));
            if ("SECRETARY".equals(role)) {
                predicates.add(cb.equal(root.get("status"), "PENDING"));
            } else if ("LEADER".equals(role)) {
                predicates.add(cb.equal(root.get("status"), "PENDING_LEADER"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return projectRepository.findAll(spec, pageable).map(this::toDTO);
    }

    private ProjectDTO toDTO(Project project) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(project.getId());
        dto.setProjectName(project.getProjectName());
        dto.setProjectLevel(project.getProjectLevel());
        dto.setProjectType(project.getProjectType());
        dto.setAdvisor(project.getAdvisor());
        dto.setMembers(project.getMembers());
        dto.setEstablishTime(project.getEstablishTime());
        dto.setProposalPath(project.getProposalPath());
        dto.setConclusionPath(project.getConclusionPath());
        dto.setCertificatePath(project.getCertificatePath());
        dto.setStatus(project.getStatus());
        dto.setApplyTime(project.getApplyTime());
        dto.setArchiveTime(project.getArchiveTime());

        if (project.getApplicant() != null) {
            dto.setApplicantId(project.getApplicant().getId());
            dto.setApplicantName(project.getApplicant().getRealName());
        }

        List<FileEntity> files = fileRepository.findByRelatedTypeAndRelatedId("PROJECT", project.getId());
        List<FileInfo> fileInfos = files.stream()
                .map(f -> {
                    FileInfo info = new FileInfo();
                    info.setId(f.getId());
                    info.setUrl("/api/files/" + f.getId());
                    info.setOriginalName(f.getOriginalName());
                    info.setFileType(f.getFileType());
                    return info;
                })
                .collect(Collectors.toList());
        dto.setFileUrlList(fileInfos);

        return dto;
    }
}
