package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.knowledge.domain.model.AiDiagnosticTrace;
import com.ocp.eia.modules.knowledge.domain.model.RetrievedDocument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AiDiagnosticTraceMapperTest {

    @Test
    void toDto_mapsRetrievalStepsAndDocuments() {
        UUID id = UUID.randomUUID();
        AiDiagnosticTrace trace = new AiDiagnosticTrace(
                "Panne variateur",
                List.of(new RetrievedDocument(id, "ABB-V01", "Surchauffe", "Isolation", 0.84)),
                15,
                7,
                7,
                1,
                0.84,
                88.0,
                95L,
                320L,
                "OK",
                true
        );

        var dto = AiDiagnosticTraceMapper.toDto(trace);

        assertEquals("Panne variateur", dto.query());
        assertEquals(1, dto.retrievedDocuments().size());
        assertEquals(84.0, dto.retrievedDocuments().get(0).similarityPercent());
        assertTrue(dto.retrievalSteps().stream().anyMatch(s -> s.step().equals("hybrid_search")));
        assertEquals("VERY_HIGH", dto.confidenceLevel());
    }

    @Test
    void toDto_withoutHybrid_omitsHybridStep() {
        AiDiagnosticTrace trace = new AiDiagnosticTrace(
                "Test",
                List.of(),
                5,
                0,
                5,
                0,
                0.0,
                0.0,
                10L,
                0L,
                "OK",
                false
        );

        var dto = AiDiagnosticTraceMapper.toDto(trace);

        assertFalse(dto.retrievalSteps().stream().anyMatch(s -> s.step().equals("hybrid_search")));
    }
}
