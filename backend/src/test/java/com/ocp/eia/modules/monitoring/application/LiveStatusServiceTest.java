package com.ocp.eia.modules.monitoring.application;

import com.ocp.eia.config.AppProperties;
import com.ocp.eia.modules.knowledge.application.RagAssistUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveStatusServiceTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private LiveEventBroadcaster broadcaster;
    @Mock private ObjectProvider<RagAssistUseCase> ragAssistUseCase;
    private AppProperties appProperties;
    private LiveStatusService service;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        service = new LiveStatusService(jdbcTemplate, broadcaster, appProperties, ragAssistUseCase);
    }

    @Test
    void currentStatus_databaseOnline_returnsOnlineDatabase() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(broadcaster.activeConnections()).thenReturn(0);
        when(ragAssistUseCase.getIfAvailable()).thenReturn(mock(RagAssistUseCase.class));
        appProperties.getKnowledge().setEnabled(true);

        var status = service.currentStatus();

        assertEquals("ONLINE", status.database().state());
        assertEquals("backend", status.backend().name());
    }

    @Test
    void currentStatus_knowledgeDisabled_returnsDegradedAiAndRag() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(broadcaster.activeConnections()).thenReturn(0);
        appProperties.getKnowledge().setEnabled(false);

        var status = service.currentStatus();

        assertEquals("DEGRADED", status.ai().state());
        assertEquals("DEGRADED", status.rag().state());
    }
}
