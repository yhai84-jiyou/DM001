package com.helpmanual.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100) String displayName
) {}
