package com.competition.repository;

import com.competition.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    List<Project> findByApplicantId(Long userId);

    long countByStatusAndIsDeleted(String status, Integer isDeleted);

    List<Project> findByStatusAndIsDeletedOrderByApplyTimeDesc(String status, Integer isDeleted, Pageable pageable);

    List<Project> findByStatusAndIsDeleted(String status, Integer isDeleted);
}
