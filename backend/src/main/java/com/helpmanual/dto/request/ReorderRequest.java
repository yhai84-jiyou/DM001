package com.helpmanual.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReorderRequest(
        @NotNull(message = "Items are required") List<ReorderItem> items
) {
    public record ReorderItem(Long id, int sortOrder) {}
}
