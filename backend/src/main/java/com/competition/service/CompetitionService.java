package com.competition.service;

import com.competition.dto.CompetitionDTO;
import com.competition.dto.CompetitionSubmitRequest;
import com.competition.dto.FileInfo;
import com.competition.entity.Competition;
import com.competition.entity.FileEntity;
import com.competition.entity.User;
import com.competition.repository.CompetitionRepository;
import com.competition.repository.FileRepository;
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
public class CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public CompetitionDTO submit(CompetitionSubmitRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Competition competition = new Competition();
        competition.setCategory(request.getCategory());
        competition.setCompetitionName(request.getCompetitionName());
        competition.setAwardLevel(request.getAwardLevel());
        competition.setAwardGrade(request.getAwardGrade());
        competition.setAwardUnit(request.getAwardUnit());
        competition.setOrganizer(request.getOrganizer());
        competition.setCoOrganizer(request.getCoOrganizer());
        competition.setAwardDate(request.getAwardDate());
        competition.setAdvisor(request.getAdvisor());
        competition.setParticipants(request.getParticipants());
        competition.setStatus("PENDING");
        competition.setApplicant(user);
        competition.setApplyTime(LocalDateTime.now());

        competition = competitionRepository.save(competition);

        // 关联文件
        if (request.getFileIds() != null && !request.getFileIds().isEmpty()) {
            fileStorageService.linkFilesToAchievement(request.getFileIds(), "COMPETITION", competition.getId());
        }

        return toDTO(competition);
    }

    public Page<CompetitionDTO> list(String category, String awardLevel, String status,
                                     int page, int size, Long userId, String role) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "applyTime"));

        Specification<Competition> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), 0));

            if (category != null && !category.isEmpty()) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (awardLevel != null && !awardLevel.isEmpty()) {
                predicates.add(cb.equal(root.get("awardLevel"), awardLevel));
            }
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            // 学生只看自己的
            if ("STUDENT".equals(role) || "TEACHER".equals(role)) {
                predicates.add(cb.equal(root.get("applicant").get("id"), userId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return competitionRepository.findAll(spec, pageable).map(this::toDTO);
    }

    public CompetitionDTO detail(Long id) {
        Competition competition = competitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("竞赛记录不存在"));
        return toDTO(competition);
    }

    @Transactional
    public CompetitionDTO update(Long id, CompetitionSubmitRequest request, Long userId) {
        Competition competition = competitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("竞赛记录不存在"));

        if (!"DRAFT".equals(competition.getStatus()) && !"REJECTED".equals(competition.getStatus())) {
            throw new RuntimeException("当前状态不可修改");
        }
        if (!competition.getApplicant().getId().equals(userId)) {
            throw new RuntimeException("只能修改自己的记录");
        }

        competition.setCategory(request.getCategory());
        competition.setCompetitionName(request.getCompetitionName());
        competition.setAwardLevel(request.getAwardLevel());
        competition.setAwardGrade(request.getAwardGrade());
        competition.setAwardUnit(request.getAwardUnit());
        competition.setOrganizer(request.getOrganizer());
        competition.setCoOrganizer(request.getCoOrganizer());
        competition.setAwardDate(request.getAwardDate());
        competition.setAdvisor(request.getAdvisor());
        competition.setParticipants(request.getParticipants());
        competition.setStatus("PENDING");

        competition = competitionRepository.save(competition);

        // 重新关联文件: 先清除旧关联，再建立新关联
        List<FileEntity> oldFiles = fileRepository.findByRelatedTypeAndRelatedId("COMPETITION", id);
        for (FileEntity f : oldFiles) {
            f.setRelatedType(null);
            f.setRelatedId(null);
            fileRepository.save(f);
        }
        if (request.getFileIds() != null && !request.getFileIds().isEmpty()) {
            fileStorageService.linkFilesToAchievement(request.getFileIds(), "COMPETITION", competition.getId());
        }

        return toDTO(competition);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Competition competition = competitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("竞赛记录不存在"));

        if (!"DRAFT".equals(competition.getStatus())) {
            throw new RuntimeException("仅草稿状态可删除");
        }
        if (!competition.getApplicant().getId().equals(userId)) {
            throw new RuntimeException("只能删除自己的记录");
        }

        competition.setIsDeleted(1);
        competitionRepository.save(competition);
    }

    public Page<CompetitionDTO> listPending(String role, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "applyTime"));

        Specification<Competition> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), 0));

            if ("SECRETARY".equals(role)) {
                predicates.add(cb.equal(root.get("status"), "PENDING"));
            } else if ("LEADER".equals(role)) {
                predicates.add(cb.equal(root.get("status"), "PENDING_LEADER"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return competitionRepository.findAll(spec, pageable).map(this::toDTO);
    }

    public Page<CompetitionDTO> listAudited(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "applyTime"));

        Specification<Competition> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), 0));
            predicates.add(cb.not(root.get("status").in("PENDING", "PENDING_LEADER")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return competitionRepository.findAll(spec, pageable).map(this::toDTO);
    }

    private CompetitionDTO toDTO(Competition competition) {
        CompetitionDTO dto = new CompetitionDTO();
        dto.setId(competition.getId());
        dto.setCategory(competition.getCategory());
        dto.setCompetitionName(competition.getCompetitionName());
        dto.setAwardLevel(competition.getAwardLevel());
        dto.setAwardGrade(competition.getAwardGrade());
        dto.setAwardUnit(competition.getAwardUnit());
        dto.setOrganizer(competition.getOrganizer());
        dto.setCoOrganizer(competition.getCoOrganizer());
        dto.setAwardDate(competition.getAwardDate());
        dto.setAdvisor(competition.getAdvisor());
        dto.setParticipants(competition.getParticipants());
        dto.setCertificatePath(competition.getCertificatePath());
        dto.setStatus(competition.getStatus());
        dto.setApplyTime(competition.getApplyTime());
        dto.setArchiveTime(competition.getArchiveTime());

        if (competition.getApplicant() != null) {
            dto.setApplicantId(competition.getApplicant().getId());
            dto.setApplicantName(competition.getApplicant().getRealName());
        }

        // 关联文件信息
        List<FileEntity> files = fileRepository.findByRelatedTypeAndRelatedId("COMPETITION", competition.getId());
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
