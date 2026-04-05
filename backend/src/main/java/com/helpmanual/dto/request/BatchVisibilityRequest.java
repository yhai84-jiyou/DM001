package com.helpmanual.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BatchVisibilityRequest(
        List<VisibilityItem> categories,
        List<VisibilityItem> articles
) {
    public record VisibilityItem(
            @NotNull Long id,
            @NotNull Boolean visible
    ) {}
}
