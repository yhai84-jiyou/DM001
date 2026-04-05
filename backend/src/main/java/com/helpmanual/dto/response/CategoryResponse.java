package com.helpmanual.dto.response;

import com.helpmanual.entity.Category;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        Long parentId,
        String name,
        String slug,
        String icon,
        int sortOrder,
        boolean visible,
        LocalDateTime createdAt
) {
    public static CategoryResponse fromEntity(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getParentId(),
                category.getName(),
                category.getSlug(),
                category.getIcon(),
                category.getSortOrder(),
                category.isVisible(),
                category.getCreatedAt()
        );
    }
}
