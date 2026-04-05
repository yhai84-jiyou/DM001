package com.helpmanual.dto.response;

import com.helpmanual.entity.Article;

import java.time.LocalDateTime;

public record PublicArticleResponse(
        Long id,
        Long categoryId,
        String title,
        String slug,
        String contentHtml,
        String summary,
        String currentVersion,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {
    public static PublicArticleResponse fromEntity(Article a) {
        return new PublicArticleResponse(
                a.getId(),
                a.getCategoryId(),
                a.getPublishedTitle() != null ? a.getPublishedTitle() : a.getTitle(),
                a.getSlug(),
                a.getPublishedContentHtml(),
                a.getPublishedSummary() != null ? a.getPublishedSummary() : a.getSummary(),
                a.getCurrentVersion(),
                a.getPublishedAt(),
                a.getUpdatedAt()
        );
    }
}
