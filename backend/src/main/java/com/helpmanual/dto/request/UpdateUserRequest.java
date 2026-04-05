package com.helpmanual.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 100) String displayName,
        String role
) {}
