package com.helpmanual.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        Long parentId,
        @NotBlank(message = "Name is required") @Size(max = 100) String name,
        @Size(max = 150) String slug,
        @Size(max = 50) String icon,
        Integer sortOrder
) {}
