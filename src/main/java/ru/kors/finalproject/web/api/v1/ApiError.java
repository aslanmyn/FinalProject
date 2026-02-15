package ru.kors.finalproject.web.api.v1;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        String code,
        String message,
        Map<String, Object> details,
        Instant timestamp
) {
    public static ApiError of(String code, String message, Map<String, Object> details) {
        return new ApiError(code, message, details, Instant.now());
    }
}
