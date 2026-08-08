package com.ocp.eia.modules.maintenance.application.event;

import com.ocp.eia.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record InterventionCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID interventionId,
        UUID failureId,
        String equipmentCode,
        String technicienNom
) implements DomainEvent {

    public InterventionCreatedEvent(
            UUID interventionId,
            UUID failureId,
            String equipmentCode,
            String technicienNom
    ) {
        this(UUID.randomUUID(), Instant.now(), interventionId, failureId, equipmentCode, technicienNom);
    }
}
