package com.ocp.eia.modules.maintenance.application.event;

import com.ocp.eia.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record InterventionKnowledgeRemovedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID interventionId
) implements DomainEvent {

    public InterventionKnowledgeRemovedEvent(UUID interventionId) {
        this(UUID.randomUUID(), Instant.now(), interventionId);
    }
}
