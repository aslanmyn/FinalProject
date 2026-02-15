package ru.kors.finalproject.web.api.v1;

import org.springframework.data.domain.Page;

import java.util.List;

public record ApiPageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
    public static <T> ApiPageResponse<T> from(Page<T> pageData) {
        return new ApiPageResponse<>(
                pageData.getContent(),
                pageData.getNumber(),
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages()
        );
    }
}
