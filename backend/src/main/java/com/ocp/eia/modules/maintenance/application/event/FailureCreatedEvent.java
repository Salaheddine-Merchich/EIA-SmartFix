package com.ocp.eia.modules.maintenance.application.event;

import com.ocp.eia.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record FailureCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID failureId,
        String equipmentCode,
        String criticite,
        String description
) implements DomainEvent {

    public FailureCreatedEvent(UUID failureId, String equipmentCode, String criticite, String description) {
        this(UUID.randomUUID(), Instant.now(), failureId, equipmentCode, criticite, description);
    }
}
