package com.ocp.eia.modules.knowledge.evaluation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RagEvaluationReportTest {

    @Test
    void fromResults_aggregatesMetrics() {
        UUID target = UUID.randomUUID();
        RagEvaluationCase evalCase = new RagEvaluationCase("c1", "question", target, "desc");
        RagEvaluationResult hit = new RagEvaluationResult(
                evalCase, List.of(target), List.of(), 1,
                true, true, true, 1.0, 0.88,
                3, 2,
                new RagEvaluationTimings(50, 30, 10, 2, 100, 192),
                true, null
        );

        RagEvaluationReport report = RagEvaluationReport.fromResults(List.of(hit));

        assertEquals(1, report.questionCount());
        assertEquals(100.0, report.precisionAt1Percent());
        assertEquals(1.0, report.meanReciprocalRank());
        assertEquals(192.0, report.avgTotalLatencyMs());
        assertEquals(50.0, report.avgEmbeddingMs());
    }

    @Test
    void toTextReport_containsKeySections() {
        RagEvaluationReport report = RagEvaluationReport.fromResults(List.of());

        String text = report.toTextReport();

        assertTrue(text.contains("Evaluation RAG"));
        assertTrue(text.contains("Top1 Accuracy"));
        assertTrue(text.contains("MRR"));
        assertTrue(text.contains("Latence moyenne"));
        assertTrue(text.contains("Vector Search"));
        assertTrue(text.contains("LLM"));
    }
}
