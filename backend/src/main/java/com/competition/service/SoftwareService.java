package com.competition.service;

import com.competition.dto.FileInfo;
import com.competition.dto.SoftwareDTO;
import com.competition.dto.SoftwareSubmitRequest;
import com.competition.entity.FileEntity;
import com.competition.entity.Software;
import com.competition.entity.User;
import com.competition.repository.FileRepository;
import com.competition.repository.SoftwareRepository;
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
public class SoftwareService {

    private final SoftwareRepository softwareRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public SoftwareDTO submit(SoftwareSubmitRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Software software = new Software();
        software.setSoftwareName(request.getSoftwareName());
        software.setAffiliation(request.getAffiliation());
        software.setCopyrightOwner(request.getCopyrightOwner());
        software.setRegistrationNumber(request.getRegistrationNumber());
        software.setRegistrationDate(request.getRegistrationDate());
        software.setStatus("PENDING");
        software.setApplicant(user);
        software.setApplyTime(LocalDateTime.now());

        software = softwareRepository.save(software);

        if (request.getFileIds() != null && !request.getFileIds().isEmpty()) {
            fileStorageService.linkFilesToAchievement(request.getFileIds(), "SOFTWARE", software.getId());
        }

        return toDTO(software);
    }

    public Page<SoftwareDTO> list(String registrationNumber, String status, int page, int size, Long userId, String role) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "applyTime"));

        Specification<Software> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), 0));

            if (registrationNumber != null && !registrationNumber.isEmpty()) {
                predicates.add(cb.like(root.get("registrationNumber"), "%" + registrationNumber + "%"));
            }
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if ("STUDENT".equals(role) || "TEACHER".equals(role)) {
                predicates.add(cb.equal(root.get("applicant").get("id"), userId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return softwareRepository.findAll(spec, pageable).map(this::toDTO);
    }

    public SoftwareDTO detail(Long id) {
        Software software = softwareRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("软著记录不存在"));
        return toDTO(software);
    }

    @Transactional
    public SoftwareDTO update(Long id, SoftwareSubmitRequest request, Long userId) {
        Software software = softwareRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("软著记录不存在"));

        if (!"REJECTED".equals(software.getStatus())) {
            throw new RuntimeException("当前状态不可修改");
        }
        if (!software.getApplicant().getId().equals(userId)) {
            throw new RuntimeException("只能修改自己的记录");
        }

        software.setSoftwareName(request.getSoftwareName());
        software.setAffiliation(request.getAffiliation());
        software.setCopyrightOwner(request.getCopyrightOwner());
        software.setRegistrationNumber(request.getRegistrationNumber());
        software.setRegistrationDate(request.getRegistrationDate());
        software.setStatus("PENDING");

        software = softwareRepository.save(software);

        List<FileEntity> oldFiles = fileRepository.findByRelatedTypeAndRelatedId("SOFTWARE", id);
        for (FileEntity f : oldFiles) {
            f.setRelatedType(null);
            f.setRelatedId(null);
            fileRepository.save(f);
        }
        if (request.getFileIds() != null && !request.getFileIds().isEmpty()) {
            fileStorageService.linkFilesToAchievement(request.getFileIds(), "SOFTWARE", software.getId());
        }

        return toDTO(software);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Software software = softwareRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("软著记录不存在"));

        if (!software.getApplicant().getId().equals(userId)) {
            throw new RuntimeException("只能删除自己的记录");
        }

        software.setIsDeleted(1);
        softwareRepository.save(software);
    }

    public Page<SoftwareDTO> listPending(String role, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "applyTime"));

        Specification<Software> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), 0));
            if ("SECRETARY".equals(role)) {
                predicates.add(cb.equal(root.get("status"), "PENDING"));
            } else if ("LEADER".equals(role)) {
                predicates.add(cb.equal(root.get("status"), "PENDING_LEADER"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return softwareRepository.findAll(spec, pageable).map(this::toDTO);
    }

    private SoftwareDTO toDTO(Software software) {
        SoftwareDTO dto = new SoftwareDTO();
        dto.setId(software.getId());
        dto.setSoftwareName(software.getSoftwareName());
        dto.setAffiliation(software.getAffiliation());
        dto.setCopyrightOwner(software.getCopyrightOwner());
        dto.setRegistrationNumber(software.getRegistrationNumber());
        dto.setMaterialPath(software.getMaterialPath());
        dto.setRegistrationDate(software.getRegistrationDate());
        dto.setCertificatePath(software.getCertificatePath());
        dto.setStatus(software.getStatus());
        dto.setApplyTime(software.getApplyTime());
        dto.setArchiveTime(software.getArchiveTime());

        if (software.getApplicant() != null) {
            dto.setApplicantId(software.getApplicant().getId());
            dto.setApplicantName(software.getApplicant().getRealName());
        }

        List<FileEntity> files = fileRepository.findByRelatedTypeAndRelatedId("SOFTWARE", software.getId());
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
