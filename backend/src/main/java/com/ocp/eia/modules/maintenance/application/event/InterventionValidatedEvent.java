package com.ocp.eia.modules.maintenance.application.event;

import com.ocp.eia.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record InterventionValidatedEvent(
        UUID eventId,
        Instant occurredAt,
        InterventionKnowledgePayload payload
) implements DomainEvent {

    public InterventionValidatedEvent(InterventionKnowledgePayload payload) {
        this(UUID.randomUUID(), Instant.now(), payload);
    }
}
