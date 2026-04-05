package com.helpmanual.dto.response;

import com.helpmanual.entity.OperationLog;

import java.time.LocalDateTime;

public record OperationLogResponse(
        Long id,
        Long userId,
        String action,
        String targetType,
        Long targetId,
        String detail,
        LocalDateTime createdAt
) {
    public static OperationLogResponse fromEntity(OperationLog log) {
        return new OperationLogResponse(
                log.getId(),
                log.getUserId(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getDetail(),
                log.getCreatedAt()
        );
    }
}
