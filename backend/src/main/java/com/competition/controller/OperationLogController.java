package com.competition.controller;

import com.competition.dto.Result;
import com.competition.entity.OperationLog;
import com.competition.service.OperationLogService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public Result<Page<OperationLog>> list(
            @RequestParam(required = false) String operationType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<OperationLog> result = operationLogService.list(operationType, page, size);
        return Result.success(result);
    }
}
