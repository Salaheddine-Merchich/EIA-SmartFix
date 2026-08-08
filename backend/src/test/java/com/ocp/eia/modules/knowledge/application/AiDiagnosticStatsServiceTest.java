package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.knowledge.domain.model.AiDiagnosticTrace;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiDiagnosticStatsServiceTest {

    @Test
    void snapshot_emptyBeforeRecord() {
        AiDiagnosticStatsService service = new AiDiagnosticStatsService();
        assertTrue(service.snapshot().isEmpty());
    }

    @Test
    void record_accumulatesDiagnosticsMetrics() {
        AiDiagnosticStatsService service = new AiDiagnosticStatsService();
        service.record(new AiDiagnosticTrace(
                "Q1", List.of(), 10, 5, 5, 2, 0.8, 82.0, 100L, 200L, "OK", true
        ));
        service.record(new AiDiagnosticTrace(
                "Q2", List.of(), 8, 4, 4, 1, 0.9, 90.0, 80L, 150L, "OK", true
        ));

        var stats = service.snapshot();
        assertTrue(stats.isPresent());
        assertEquals(2, stats.get().diagnosticsCount());
        assertEquals(3, stats.get().totalRetrievals());
        assertEquals(86.0, stats.get().averageConfidence());
    }
}
