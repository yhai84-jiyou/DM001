package com.helpmanual.dto.response;

import com.helpmanual.entity.ArticleVersion;

import java.time.LocalDateTime;

public record ArticleVersionResponse(
        Long id,
        Long articleId,
        String versionLabel,
        String changeNotes,
        String title,
        String content,
        String contentHtml,
        Long publishedBy,
        LocalDateTime createdAt
) {
    public static ArticleVersionResponse fromEntity(ArticleVersion v) {
        return new ArticleVersionResponse(
                v.getId(),
                v.getArticleId(),
                v.getVersionLabel(),
                v.getChangeNotes(),
                v.getTitle(),
                v.getContent(),
                v.getContentHtml(),
                v.getPublishedBy(),
                v.getCreatedAt()
        );
    }
}
