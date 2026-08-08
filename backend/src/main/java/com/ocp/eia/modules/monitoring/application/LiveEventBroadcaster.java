package com.ocp.eia.modules.monitoring.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocp.eia.application.dto.LiveDto.LiveEventResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveEventBroadcaster {

    private final ObjectMapper objectMapper;
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe(long timeoutMs) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        sendRaw(emitter, "connected", Map.of("message", "live stream connected"));
        return emitter;
    }

    public void broadcast(LiveEventResponse event) {
        emitters.forEach(emitter -> sendEvent(emitter, event));
    }

    public void broadcastHeartbeat() {
        emitters.forEach(emitter -> sendRaw(emitter, "heartbeat", Map.of("ts", System.currentTimeMillis())));
    }

    public int activeConnections() {
        return emitters.size();
    }

    private void sendEvent(SseEmitter emitter, LiveEventResponse event) {
        try {
            emitter.send(SseEmitter.event()
                    .name("live-event")
                    .id(event.id().toString())
                    .data(toJson(event)));
        } catch (IOException e) {
            emitters.remove(emitter);
            log.debug("SSE client disconnected: {}", e.getMessage());
        }
    }

    private void sendRaw(SseEmitter emitter, String name, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(name).data(toJson(payload)));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
