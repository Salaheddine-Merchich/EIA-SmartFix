package com.ocp.eia.modules.knowledge.domain.model;

import java.util.List;

public record AiDiagnosticTrace(
        String query,
        List<RetrievedDocument> retrievedDocuments,
        int vectorResultCount,
        int textResultCount,
        int mergedResultCount,
        int filteredCount,
        double averageSimilarity,
        double confidenceScore,
        long retrievalDurationMs,
        long llmDurationMs,
        String embeddingStatus,
        boolean hybridSearchEnabled
) {}
