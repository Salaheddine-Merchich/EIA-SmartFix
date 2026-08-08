package com.ocp.eia.modules.monitoring.application.event;

import com.ocp.eia.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record AiServiceUnavailableEvent(
        UUID eventId,
        Instant occurredAt,
        String reason
) implements DomainEvent {

    public AiServiceUnavailableEvent(String reason) {
        this(UUID.randomUUID(), Instant.now(), reason);
    }
}
