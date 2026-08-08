package com.ocp.eia.modules.monitoring.application;

import com.ocp.eia.config.AppProperties;
import com.ocp.eia.modules.maintenance.application.event.FailureCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LiveMonitoringListenerTest {

    @Mock private LiveMonitoringService liveMonitoringService;
    @InjectMocks private LiveMonitoringListener listener;

    @Test
    void onFailureCreated_delegatesToService() {
        FailureCreatedEvent event = new FailureCreatedEvent(UUID.randomUUID(), "EQ-1", "HAUTE", "Desc");

        listener.onFailureCreated(event);

        verify(liveMonitoringService).publishFailureCreated(event);
    }
}
