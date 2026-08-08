package com.ocp.eia.presentation.controller;

import com.ocp.eia.application.dto.LiveDto.LiveStatusResponse;
import com.ocp.eia.modules.monitoring.application.LiveEventBroadcaster;
import com.ocp.eia.modules.monitoring.application.LiveStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/live")
@RequiredArgsConstructor
@Tag(name = "Live Monitoring")
public class LiveMonitoringController {

    private static final long SSE_TIMEOUT_MS = 30L * 60L * 1000L;

    private final LiveEventBroadcaster broadcaster;
    private final LiveStatusService statusService;

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Flux SSE des événements temps réel")
    public SseEmitter streamEvents() {
        return broadcaster.subscribe(SSE_TIMEOUT_MS);
    }

    @GetMapping("/status")
    @Operation(summary = "Statut des services pour la barre de monitoring")
    public ResponseEntity<LiveStatusResponse> status() {
        return ResponseEntity.ok(statusService.currentStatus());
    }
}
