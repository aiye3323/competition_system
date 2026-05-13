package com.competition.repository;

import com.competition.entity.Paper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaperRepository extends JpaRepository<Paper, Long>, JpaSpecificationExecutor<Paper> {

    List<Paper> findByApplicantId(Long userId);

    long countByStatusAndIsDeleted(String status, Integer isDeleted);

    List<Paper> findByStatusAndIsDeletedOrderByApplyTimeDesc(String status, Integer isDeleted, Pageable pageable);

    List<Paper> findByStatusAndIsDeleted(String status, Integer isDeleted);
}
