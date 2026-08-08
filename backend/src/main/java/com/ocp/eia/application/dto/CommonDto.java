package com.ocp.eia.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class CommonDto {

    private CommonDto() {}

    public record ApiErrorResponse(
            Instant timestamp,
            int status,
            String code,
            String message,
            Map<String, String> details
    ) {}

    public record PageResponse<T>(
            List<T> content,
            long totalElements,
            int totalPages,
            int page,
            int size
    ) {}
}
