package com.ocp.eia.modules.maintenance.application.event;

import com.ocp.eia.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when indexed fields of a still-{@code VALIDEE} intervention change
 * (e.g. linked failure/equipment metadata), so the knowledge module can re-embed.
 */
public record InterventionKnowledgeChangedEvent(
        UUID eventId,
        Instant occurredAt,
        InterventionKnowledgePayload payload
) implements DomainEvent {

    public InterventionKnowledgeChangedEvent(InterventionKnowledgePayload payload) {
        this(UUID.randomUUID(), Instant.now(), payload);
    }
}
