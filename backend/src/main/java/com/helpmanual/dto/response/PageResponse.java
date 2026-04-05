package com.helpmanual.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <E, T> PageResponse<T> from(Page<E> pageData, Function<E, T> mapper) {
        return new PageResponse<>(
                pageData.getContent().stream().map(mapper).toList(),
                pageData.getNumber(),
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages()
        );
    }
}
