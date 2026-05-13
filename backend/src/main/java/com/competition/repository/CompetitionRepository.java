package com.competition.repository;

import com.competition.entity.Competition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CompetitionRepository extends JpaRepository<Competition, Long>, JpaSpecificationExecutor<Competition> {

    List<Competition> findByApplicantId(Long userId);

    long countByStatusAndIsDeleted(String status, Integer isDeleted);

    List<Competition> findByStatusAndIsDeletedOrderByApplyTimeDesc(String status, Integer isDeleted, Pageable pageable);

    List<Competition> findByStatusAndIsDeleted(String status, Integer isDeleted);
}
