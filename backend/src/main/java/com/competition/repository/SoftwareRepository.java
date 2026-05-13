package com.competition.repository;

import com.competition.entity.Software;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface SoftwareRepository extends JpaRepository<Software, Long>, JpaSpecificationExecutor<Software> {

    List<Software> findByApplicantId(Long userId);

    long countByStatusAndIsDeleted(String status, Integer isDeleted);

    List<Software> findByStatusAndIsDeletedOrderByApplyTimeDesc(String status, Integer isDeleted, Pageable pageable);

    List<Software> findByStatusAndIsDeleted(String status, Integer isDeleted);
}
