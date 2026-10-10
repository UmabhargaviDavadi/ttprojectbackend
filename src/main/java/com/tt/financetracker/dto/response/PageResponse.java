package com.tt.financetracker.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int page, int size) {
    public static <S, T> PageResponse<T> from(Page<S> result, Function<S, T> mapper) {
        return new PageResponse<>(result.getContent().stream().map(mapper).toList(), result.getTotalElements(),
                result.getTotalPages(), result.getNumber(), result.getSize());
    }
}
