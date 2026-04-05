package com.helpmanual.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ArticleRequest(
        Long categoryId,
        @NotBlank(message = "Title is required") @Size(max = 255) String title,
        @Size(max = 300) String slug,
        String draftContent,
        String draftContentHtml,
        @Size(max = 500) String summary,
        String editorMode,
        Integer sortOrder
) {}
