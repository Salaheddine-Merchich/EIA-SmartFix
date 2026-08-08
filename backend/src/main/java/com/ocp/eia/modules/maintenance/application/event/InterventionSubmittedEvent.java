package com.ocp.eia.modules.maintenance.application.event;

import com.ocp.eia.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record InterventionSubmittedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID interventionId,
        String equipmentCode,
        String technicienNom
) implements DomainEvent {

    public InterventionSubmittedEvent(UUID interventionId, String equipmentCode, String technicienNom) {
        this(UUID.randomUUID(), Instant.now(), interventionId, equipmentCode, technicienNom);
    }
}
