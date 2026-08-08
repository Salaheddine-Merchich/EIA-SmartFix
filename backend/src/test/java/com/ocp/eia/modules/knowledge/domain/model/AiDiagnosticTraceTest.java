package com.ocp.eia.modules.knowledge.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AiDiagnosticTraceTest {

    @Test
    void trace_holdsRetrievalMetrics() {
        UUID id = UUID.randomUUID();
        RetrievedDocument doc = new RetrievedDocument(id, "CV-101", "Surchauffe", "Code E001", 0.91);

        AiDiagnosticTrace trace = new AiDiagnosticTrace(
                "Panne convoyeur",
                List.of(doc),
                15,
                7,
                7,
                3,
                0.87,
                88.5,
                120L,
                450L,
                "OK",
                true
        );

        assertEquals("Panne convoyeur", trace.query());
        assertEquals(1, trace.retrievedDocuments().size());
        assertEquals(15, trace.vectorResultCount());
        assertEquals(3, trace.filteredCount());
        assertEquals(88.5, trace.confidenceScore());
        assertEquals("OK", trace.embeddingStatus());
    }
}
