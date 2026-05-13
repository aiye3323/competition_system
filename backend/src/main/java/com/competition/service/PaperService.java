package com.competition.service;

import com.competition.dto.FileInfo;
import com.competition.dto.PaperDTO;
import com.competition.dto.PaperSubmitRequest;
import com.competition.entity.FileEntity;
import com.competition.entity.Paper;
import com.competition.entity.User;
import com.competition.repository.FileRepository;
import com.competition.repository.PaperRepository;
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
public class PaperService {

    private final PaperRepository paperRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public PaperDTO submit(PaperSubmitRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Paper paper = new Paper();
        paper.setTitle(request.getTitle());
        paper.setJournalLevel(request.getJournalLevel());
        paper.setJournalName(request.getJournalName());
        paper.setKeywords(request.getKeywords());
        paper.setAuthors(request.getAuthors());
        paper.setSubmissionDate(request.getSubmissionDate());
        paper.setAcceptanceDate(request.getAcceptanceDate());
        paper.setStatus("PENDING");
        paper.setApplicant(user);
        paper.setApplyTime(LocalDateTime.now());

        paper = paperRepository.save(paper);

        if (request.getFileIds() != null && !request.getFileIds().isEmpty()) {
            fileStorageService.linkFilesToAchievement(request.getFileIds(), "PAPER", paper.getId());
        }

        return toDTO(paper);
    }

    public Page<PaperDTO> list(String journalLevel, String status, int page, int size, Long userId, String role) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "applyTime"));

        Specification<Paper> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), 0));

            if (journalLevel != null && !journalLevel.isEmpty()) {
                predicates.add(cb.equal(root.get("journalLevel"), journalLevel));
            }
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if ("STUDENT".equals(role) || "TEACHER".equals(role)) {
                predicates.add(cb.equal(root.get("applicant").get("id"), userId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return paperRepository.findAll(spec, pageable).map(this::toDTO);
    }

    public PaperDTO detail(Long id) {
        Paper paper = paperRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("论文记录不存在"));
        return toDTO(paper);
    }

    @Transactional
    public PaperDTO update(Long id, PaperSubmitRequest request, Long userId) {
        Paper paper = paperRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("论文记录不存在"));

        if (!"REJECTED".equals(paper.getStatus())) {
            throw new RuntimeException("当前状态不可修改");
        }
        if (!paper.getApplicant().getId().equals(userId)) {
            throw new RuntimeException("只能修改自己的记录");
        }

        paper.setTitle(request.getTitle());
        paper.setJournalLevel(request.getJournalLevel());
        paper.setJournalName(request.getJournalName());
        paper.setKeywords(request.getKeywords());
        paper.setAuthors(request.getAuthors());
        paper.setSubmissionDate(request.getSubmissionDate());
        paper.setAcceptanceDate(request.getAcceptanceDate());
        paper.setStatus("PENDING");

        paper = paperRepository.save(paper);

        List<FileEntity> oldFiles = fileRepository.findByRelatedTypeAndRelatedId("PAPER", id);
        for (FileEntity f : oldFiles) {
            f.setRelatedType(null);
            f.setRelatedId(null);
            fileRepository.save(f);
        }
        if (request.getFileIds() != null && !request.getFileIds().isEmpty()) {
            fileStorageService.linkFilesToAchievement(request.getFileIds(), "PAPER", paper.getId());
        }

        return toDTO(paper);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Paper paper = paperRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("论文记录不存在"));

        if (!paper.getApplicant().getId().equals(userId)) {
            throw new RuntimeException("只能删除自己的记录");
        }

        paper.setIsDeleted(1);
        paperRepository.save(paper);
    }

    public Page<PaperDTO> listPending(String role, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "applyTime"));

        Specification<Paper> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), 0));
            if ("SECRETARY".equals(role)) {
                predicates.add(cb.equal(root.get("status"), "PENDING"));
            } else if ("LEADER".equals(role)) {
                predicates.add(cb.equal(root.get("status"), "PENDING_LEADER"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return paperRepository.findAll(spec, pageable).map(this::toDTO);
    }

    private PaperDTO toDTO(Paper paper) {
        PaperDTO dto = new PaperDTO();
        dto.setId(paper.getId());
        dto.setTitle(paper.getTitle());
        dto.setJournalLevel(paper.getJournalLevel());
        dto.setJournalName(paper.getJournalName());
        dto.setKeywords(paper.getKeywords());
        dto.setAuthors(paper.getAuthors());
        dto.setSubmissionDate(paper.getSubmissionDate());
        dto.setAcceptanceDate(paper.getAcceptanceDate());
        dto.setStatus(paper.getStatus());
        dto.setApplyTime(paper.getApplyTime());
        dto.setArchiveTime(paper.getArchiveTime());

        if (paper.getApplicant() != null) {
            dto.setApplicantId(paper.getApplicant().getId());
            dto.setApplicantName(paper.getApplicant().getRealName());
        }

        List<FileEntity> files = fileRepository.findByRelatedTypeAndRelatedId("PAPER", paper.getId());
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
