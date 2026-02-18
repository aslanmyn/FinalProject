package ru.kors.finalproject.web.api.v1;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ApiPageableFactory {

    public PageRequest create(
            int page,
            int size,
            String sortBy,
            String direction,
            String defaultSortBy,
            Set<String> allowedSortFields) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        String sortField = (sortBy != null && allowedSortFields.contains(sortBy))
                ? sortBy
                : defaultSortBy;
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(safePage, safeSize, Sort.by(sortDirection, sortField));
    }
}
