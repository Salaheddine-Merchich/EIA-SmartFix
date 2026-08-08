package com.ocp.eia.modules.monitoring.config;

import com.ocp.eia.modules.monitoring.application.LiveEventBroadcaster;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class LiveMonitoringScheduler {

    private final LiveEventBroadcaster broadcaster;

    @Scheduled(fixedRate = 30_000)
    public void heartbeat() {
        broadcaster.broadcastHeartbeat();
    }
}
