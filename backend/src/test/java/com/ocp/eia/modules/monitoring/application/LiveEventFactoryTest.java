package com.ocp.eia.modules.monitoring.application;

import com.ocp.eia.application.dto.LiveDto.LiveEventResponse;
import com.ocp.eia.modules.maintenance.application.event.FailureCreatedEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LiveEventFactoryTest {

    private final LiveEventFactory factory = new LiveEventFactory();

    @Test
    void fromFailureCreated_criticalCriticite_mapsToCriticalAlert() {
        FailureCreatedEvent event = new FailureCreatedEvent(
                UUID.randomUUID(), "EQ-100", "CRITIQUE", "Fuite hydraulique"
        );

        LiveEventResponse response = factory.fromFailureCreated(event);

        assertEquals("CRITICAL_ALERT", response.type());
        assertEquals("alert", response.category());
        assertTrue(response.message().contains("EQ-100"));
    }

    @Test
    void fromFailureCreated_normalCriticite_mapsToFailureCreated() {
        FailureCreatedEvent event = new FailureCreatedEvent(
                UUID.randomUUID(), "EQ-200", "MOYENNE", "Vibration"
        );

        LiveEventResponse response = factory.fromFailureCreated(event);

        assertEquals("FAILURE_CREATED", response.type());
        assertEquals("maintenance", response.category());
    }

    @Test
    void fromAiUnavailable_setsAiCategory() {
        var event = new com.ocp.eia.modules.monitoring.application.event.AiServiceUnavailableEvent("Embedding indisponible");

        LiveEventResponse response = factory.fromAiUnavailable(event);

        assertEquals("AI_UNAVAILABLE", response.type());
        assertEquals("ai", response.category());
        assertEquals("Service d'assistance IA temporairement indisponible", response.message());
    }
}
