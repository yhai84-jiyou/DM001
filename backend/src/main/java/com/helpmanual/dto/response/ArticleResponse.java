package com.helpmanual.dto.response;

import com.helpmanual.entity.Article;

import java.time.LocalDateTime;

public record ArticleResponse(
        Long id,
        Long categoryId,
        String title,
        String slug,
        String draftContent,
        String draftContentHtml,
        String publishedTitle,
        String publishedContentHtml,
        String publishedSummary,
        String summary,
        String status,
        String editorMode,
        Long currentVersion,
        boolean visible,
        boolean deleted,
        int sortOrder,
        Long authorId,
        Long lastEditorId,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt,
        LocalDateTime createdAt
) {
    public static ArticleResponse fromEntity(Article a) {
        return new ArticleResponse(
                a.getId(),
                a.getCategoryId(),
                a.getTitle(),
                a.getSlug(),
                a.getDraftContent(),
                a.getDraftContentHtml(),
                a.getPublishedTitle(),
                a.getPublishedContentHtml(),
                a.getPublishedSummary(),
                a.getSummary(),
                a.getStatus().name(),
                a.getEditorMode().name(),
                a.getCurrentVersion(),
                a.isVisible(),
                a.isDeleted(),
                a.getSortOrder(),
                a.getAuthorId(),
                a.getLastEditorId(),
                a.getPublishedAt(),
                a.getUpdatedAt(),
                a.getCreatedAt()
        );
    }
}
