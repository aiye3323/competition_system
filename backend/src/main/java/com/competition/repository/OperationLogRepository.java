package com.competition.repository;

import com.competition.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    Page<OperationLog> findAllByOrderByOperateTimeDesc(Pageable pageable);

    Page<OperationLog> findByOperationTypeOrderByOperateTimeDesc(String operationType, Pageable pageable);

    Page<OperationLog> findByUserIdOrderByOperateTimeDesc(Long userId, Pageable pageable);

    Page<OperationLog> findByOperateTimeBetweenOrderByOperateTimeDesc(
            LocalDateTime start, LocalDateTime end, Pageable pageable);
}
