package com.ocp.eia.modules.monitoring.application;

import com.ocp.eia.application.dto.LiveDto.LiveEventResponse;
import com.ocp.eia.modules.maintenance.application.event.FailureCreatedEvent;
import com.ocp.eia.modules.monitoring.application.event.AiServiceUnavailableEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveMonitoringServiceTest {

    @Mock private LiveEventBroadcaster broadcaster;
    @Mock private LiveEventFactory factory;
    @InjectMocks private LiveMonitoringService service;

    @Test
    void publishFailureCreated_broadcastsMappedEvent() {
        FailureCreatedEvent event = new FailureCreatedEvent(UUID.randomUUID(), "EQ-1", "HAUTE", "Desc");
        LiveEventResponse mapped = new LiveEventResponse(
                UUID.randomUUID(), "CRITICAL_ALERT", "alert", "Alerte", "Msg", Instant.now(), Map.of()
        );
        when(factory.fromFailureCreated(event)).thenReturn(mapped);

        service.publishFailureCreated(event);

        verify(broadcaster).broadcast(mapped);
    }

    @Test
    void publishAiUnavailable_broadcastsMappedEvent() {
        AiServiceUnavailableEvent event = new AiServiceUnavailableEvent("Embedding indisponible");
        LiveEventResponse mapped = new LiveEventResponse(
                UUID.randomUUID(), "AI_UNAVAILABLE", "ai", "IA", "Embedding", Instant.now(), Map.of()
        );
        when(factory.fromAiUnavailable(event)).thenReturn(mapped);

        service.publishAiUnavailable(event);

        verify(broadcaster).broadcast(mapped);
    }
}
