package com.competition.service;

import com.competition.entity.OperationLog;
import com.competition.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    @Transactional
    public void log(Long userId, String username, String operationType, String targetType,
                    Long targetId, String description, String ip) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setOperationType(operationType);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDescription(description);
        log.setIp(ip);
        log.setOperateTime(java.time.LocalDateTime.now());
        operationLogRepository.save(log);
    }

    public Page<OperationLog> list(String operationType, int page, int size) {
        PageRequest pr = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "operateTime"));
        if (operationType != null && !operationType.isEmpty()) {
            return operationLogRepository.findByOperationTypeOrderByOperateTimeDesc(operationType, pr);
        }
        return operationLogRepository.findAllByOrderByOperateTimeDesc(pr);
    }
}
