package com.helpmanual.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetupRequest(
        @NotBlank(message = "Username is required") @Size(min = 3, max = 50) String username,
        @NotBlank(message = "Password is required") @Size(min = 6, max = 100) String password,
        @NotBlank(message = "Display name is required") @Size(max = 100) String displayName,
        Boolean seedData
) {}
