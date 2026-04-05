package com.helpmanual.dto.request;

import jakarta.validation.constraints.NotNull;

public record VisibilityRequest(
        @NotNull(message = "Visible flag is required") Boolean visible
) {}
