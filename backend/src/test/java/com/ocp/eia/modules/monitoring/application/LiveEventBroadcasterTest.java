package com.ocp.eia.modules.monitoring.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocp.eia.application.dto.LiveDto.LiveEventResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LiveEventBroadcasterTest {

    private LiveEventBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        broadcaster = new LiveEventBroadcaster(new ObjectMapper());
    }

    @Test
    void subscribe_incrementsActiveConnections() {
        SseEmitter emitter = broadcaster.subscribe(60_000L);
        assertNotNull(emitter);
        assertEquals(1, broadcaster.activeConnections());
    }

    @Test
    void broadcast_withNoSubscribers_doesNotThrow() {
        LiveEventResponse event = new LiveEventResponse(
                UUID.randomUUID(),
                "FAILURE_CREATED",
                "maintenance",
                "Test",
                "Message",
                Instant.now(),
                Map.of()
        );

        assertDoesNotThrow(() -> broadcaster.broadcast(event));
    }

    @Test
    void broadcastHeartbeat_withSubscriber_doesNotThrow() {
        broadcaster.subscribe(60_000L);
        assertDoesNotThrow(() -> broadcaster.broadcastHeartbeat());
    }
}
