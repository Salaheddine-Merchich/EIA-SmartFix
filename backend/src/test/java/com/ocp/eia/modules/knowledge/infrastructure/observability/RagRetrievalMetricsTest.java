package com.ocp.eia.modules.knowledge.infrastructure.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RagRetrievalMetricsTest {

    private SimpleMeterRegistry registry;
    private RagRetrievalMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new RagRetrievalMetrics(registry);
    }

    @Test
    void recordVectorCount_incrementsCounter() {
        metrics.recordVectorCount(3);
        metrics.recordVectorCount(2);

        assertEquals(5.0, registry.get("rag.retrieval.vector.count").counter().count());
    }

    @Test
    void recordTextCount_incrementsCounter() {
        metrics.recordTextCount(4);

        assertEquals(4.0, registry.get("rag.retrieval.text.count").counter().count());
    }

    @Test
    void recordMergedCount_incrementsCounter() {
        metrics.recordMergedCount(2);

        assertEquals(2.0, registry.get("rag.retrieval.merged.count").counter().count());
    }

    @Test
    void recordFilteredCount_incrementsCounter() {
        metrics.recordFilteredCount(1);

        assertEquals(1.0, registry.get("rag.retrieval.filtered.count").counter().count());
    }

    @Test
    void recordLlmCall_incrementsCounter() {
        metrics.recordLlmCall();

        assertEquals(1.0, registry.get("rag.retrieval.llm.calls").counter().count());
    }

    @Test
    void recordRetrievalDuration_recordsTimer() {
        var sample = metrics.startRetrievalTimer();
        metrics.recordRetrievalDuration(sample);

        assertEquals(1, registry.get("rag.retrieval.duration").timer().count());
    }
}
