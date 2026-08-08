package com.ocp.eia.application.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class LiveDto {

    private LiveDto() {}

    public record LiveEventResponse(
            UUID id,
            String type,
            String category,
            String title,
            String message,
            Instant occurredAt,
            Map<String, String> metadata
    ) {}

    public record ServiceStatusResponse(
            String name,
            String state
    ) {}

    public record LiveStatusResponse(
            ServiceStatusResponse backend,
            ServiceStatusResponse database,
            ServiceStatusResponse ai,
            ServiceStatusResponse rag,
            ServiceStatusResponse liveStream,
            Instant checkedAt
    ) {}
}
