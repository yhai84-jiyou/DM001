package com.helpmanual.controller;

import com.helpmanual.dto.response.OperationLogResponse;
import com.helpmanual.dto.response.PageResponse;
import com.helpmanual.service.OperationLogService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/logs")
public class AdminLogController {

    private final OperationLogService operationLogService;

    public AdminLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<OperationLogResponse>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var logs = operationLogService.list(userId, action, targetType,
                PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.from(logs, OperationLogResponse::fromEntity));
    }
}
