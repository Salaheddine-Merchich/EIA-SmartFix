package com.ocp.eia.modules.monitoring.application.event;

import com.ocp.eia.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record RagIndexCompletedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID interventionId,
        String outcome,
        String equipmentCode
) implements DomainEvent {

    public RagIndexCompletedEvent(UUID interventionId, String outcome, String equipmentCode) {
        this(UUID.randomUUID(), Instant.now(), interventionId, outcome, equipmentCode);
    }
}
