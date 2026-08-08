package com.ocp.eia.modules.monitoring.application;

import com.ocp.eia.application.dto.LiveDto.LiveStatusResponse;
import com.ocp.eia.application.dto.LiveDto.ServiceStatusResponse;
import com.ocp.eia.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LiveStatusService {

    private final JdbcTemplate jdbcTemplate;
    private final LiveEventBroadcaster broadcaster;
    private final AppProperties appProperties;
    private final ObjectProvider<com.ocp.eia.modules.knowledge.application.RagAssistUseCase> ragAssistUseCase;

    public LiveStatusResponse currentStatus() {
        return new LiveStatusResponse(
                online("backend"),
                databaseStatus(),
                aiStatus(),
                ragStatus(),
                liveStreamStatus(),
                Instant.now()
        );
    }

    private ServiceStatusResponse databaseStatus() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return online("database");
        } catch (Exception e) {
            return offline("database");
        }
    }

    private ServiceStatusResponse aiStatus() {
        if (!appProperties.getKnowledge().isEnabled()) {
            return degraded("ai");
        }
        return ragAssistUseCase.getIfAvailable() != null ? online("ai") : degraded("ai");
    }

    private ServiceStatusResponse ragStatus() {
        if (!appProperties.getKnowledge().isEnabled()) {
            return degraded("rag");
        }
        return ragAssistUseCase.getIfAvailable() != null ? online("rag") : offline("rag");
    }

    private ServiceStatusResponse liveStreamStatus() {
        int connections = broadcaster.activeConnections();
        if (connections > 0) {
            return online("liveStream");
        }
        return degraded("liveStream");
    }

    private static ServiceStatusResponse online(String name) {
        return new ServiceStatusResponse(name, "ONLINE");
    }

    private static ServiceStatusResponse degraded(String name) {
        return new ServiceStatusResponse(name, "DEGRADED");
    }

    private static ServiceStatusResponse offline(String name) {
        return new ServiceStatusResponse(name, "OFFLINE");
    }
}
