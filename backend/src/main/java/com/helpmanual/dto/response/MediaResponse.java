package com.helpmanual.dto.response;

import com.helpmanual.entity.Media;

import java.time.LocalDateTime;

public record MediaResponse(
        Long id,
        String originalName,
        String storedName,
        String filePath,
        Long fileSize,
        String contentType,
        Long uploadedBy,
        String url,
        LocalDateTime createdAt
) {
    public static MediaResponse fromEntity(Media m) {
        return new MediaResponse(
                m.getId(),
                m.getOriginalName(),
                m.getStoredName(),
                m.getFilePath(),
                m.getFileSize(),
                m.getContentType(),
                m.getUploadedBy(),
                "/uploads/" + m.getStoredName(),
                m.getCreatedAt()
        );
    }
}
