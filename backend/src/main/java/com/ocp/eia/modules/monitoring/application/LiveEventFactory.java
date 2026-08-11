package com.ocp.eia.modules.monitoring.application;

import com.ocp.eia.application.dto.LiveDto.LiveEventResponse;
import com.ocp.eia.modules.maintenance.application.event.*;
import com.ocp.eia.modules.monitoring.application.event.AiServiceUnavailableEvent;
import com.ocp.eia.modules.monitoring.application.event.RagIndexCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LiveEventFactory {

    public LiveEventResponse fromFailureCreated(FailureCreatedEvent event) {
        boolean critical = "CRITIQUE".equals(event.criticite()) || "HAUTE".equals(event.criticite());
        return new LiveEventResponse(
                event.eventId(),
                critical ? "CRITICAL_ALERT" : "FAILURE_CREATED",
                critical ? "alert" : "maintenance",
                critical ? "Nouvelle alerte critique" : "Nouvelle panne",
                formatMessage(event.equipmentCode(), event.description()),
                event.occurredAt(),
                Map.of(
                        "failureId", event.failureId().toString(),
                        "equipmentCode", nullToEmpty(event.equipmentCode()),
                        "criticite", nullToEmpty(event.criticite())
                )
        );
    }

    public LiveEventResponse fromInterventionCreated(InterventionCreatedEvent event) {
        return new LiveEventResponse(
                event.eventId(),
                "INTERVENTION_CREATED",
                "maintenance",
                "Intervention créée",
                event.equipmentCode() + " — " + nullToEmpty(event.technicienNom()),
                event.occurredAt(),
                Map.of(
                        "interventionId", event.interventionId().toString(),
                        "failureId", event.failureId().toString(),
                        "equipmentCode", nullToEmpty(event.equipmentCode())
                )
        );
    }

    public LiveEventResponse fromInterventionSubmitted(InterventionSubmittedEvent event) {
        return new LiveEventResponse(
                event.eventId(),
                "INTERVENTION_SUBMITTED",
                "maintenance",
                "Intervention soumise",
                event.equipmentCode() + " — en attente de validation",
                event.occurredAt(),
                Map.of(
                        "interventionId", event.interventionId().toString(),
                        "equipmentCode", nullToEmpty(event.equipmentCode())
                )
        );
    }

    public LiveEventResponse fromInterventionValidated(InterventionValidatedEvent event) {
        var payload = event.payload();
        return new LiveEventResponse(
                event.eventId(),
                "INTERVENTION_VALIDATED",
                "maintenance",
                "Intervention validée",
                nullToEmpty(payload.equipmentCode()) + " — indexation RAG en cours",
                event.occurredAt(),
                Map.of(
                        "interventionId", payload.interventionId().toString(),
                        "equipmentCode", nullToEmpty(payload.equipmentCode())
                )
        );
    }

    public LiveEventResponse fromRagIndexCompleted(RagIndexCompletedEvent event) {
        return new LiveEventResponse(
                event.eventId(),
                "RAG_REINDEXED",
                "knowledge",
                "RAG réindexé",
                event.equipmentCode() + " — " + event.outcome(),
                event.occurredAt(),
                Map.of(
                        "interventionId", event.interventionId().toString(),
                        "outcome", event.outcome()
                )
        );
    }

    public LiveEventResponse fromAiUnavailable(AiServiceUnavailableEvent event) {
        return new LiveEventResponse(
                event.eventId(),
                "AI_UNAVAILABLE",
                "ai",
                "IA indisponible",
                "Service d'assistance IA temporairement indisponible",
                event.occurredAt(),
                Map.of()
        );
    }

    public LiveEventResponse statusSnapshot(UUID id, String message) {
        return new LiveEventResponse(
                id,
                "STATUS_UPDATE",
                "system",
                "Statut système",
                message,
                java.time.Instant.now(),
                Map.of()
        );
    }

    /** Avoid broadcasting free-text failure descriptions on the plant-wide SSE feed. */
    private static String formatMessage(String equipmentCode, String description) {
        return nullToEmpty(equipmentCode) + " — incident enregistré";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
