package com.helpmanual.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Username is required") @Size(min = 3, max = 50) String username,
        @NotBlank(message = "Display name is required") @Size(max = 100) String displayName,
        @NotBlank(message = "Role is required") String role
) {}
