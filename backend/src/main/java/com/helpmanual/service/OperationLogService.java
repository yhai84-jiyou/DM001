package com.helpmanual.service;

import com.helpmanual.entity.OperationLog;
import com.helpmanual.repository.OperationLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    public OperationLogService(OperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    public void log(Long userId, String action, String targetType, Long targetId, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        operationLogRepository.save(log);
    }

    public Page<OperationLog> list(Long userId, String action, String targetType, Pageable pageable) {
        return operationLogRepository.findWithFilters(userId, action, targetType, pageable);
    }
}
