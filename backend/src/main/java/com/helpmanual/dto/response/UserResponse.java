package com.helpmanual.dto.response;

import com.helpmanual.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String displayName,
        String role,
        boolean forcePasswordChange,
        String status,
        LocalDateTime createdAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole().name(),
                user.isForcePasswordChange(),
                user.getStatus().name(),
                user.getCreatedAt()
        );
    }
}
