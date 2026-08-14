package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.application.dto.AiDto.AiDiagnosticTraceDto;
import com.ocp.eia.application.dto.AiDto.EquipmentSchemaDto;
import com.ocp.eia.application.dto.AiDto.RetrievalStepDto;
import com.ocp.eia.application.dto.AiDto.RetrievedDocumentDto;
import com.ocp.eia.modules.knowledge.domain.model.AiDiagnosticTrace;
import com.ocp.eia.modules.knowledge.domain.model.RetrievedDocument;

import java.util.ArrayList;
import java.util.List;

public final class AiDiagnosticTraceMapper {

    private AiDiagnosticTraceMapper() {}

    public static AiDiagnosticTraceDto toDto(AiDiagnosticTrace trace) {
        return toDto(trace, List.of());
    }

    public static AiDiagnosticTraceDto toDto(AiDiagnosticTrace trace, List<EquipmentSchemaDto> schemas) {
        List<RetrievedDocumentDto> documents = trace.retrievedDocuments().stream()
                .map(AiDiagnosticTraceMapper::toDocumentDto)
                .toList();

        return new AiDiagnosticTraceDto(
                trace.query(),
                documents,
                AiDiagnosticTraceFactory.toRetrievedSchemaDtos(schemas),
                buildRetrievalSteps(trace),
                trace.vectorResultCount(),
                trace.textResultCount(),
                trace.mergedResultCount(),
                trace.filteredCount(),
                round(trace.averageSimilarity()),
                trace.confidenceScore(),
                ConfidenceCalculator.level(trace.confidenceScore()),
                trace.retrievalDurationMs(),
                trace.llmDurationMs()
        );
    }

    private static RetrievedDocumentDto toDocumentDto(RetrievedDocument doc) {
        String title = doc.symptomes() != null && !doc.symptomes().isBlank()
                ? doc.symptomes()
                : "Intervention validée";
        String detail = doc.causeRacine() != null && !doc.causeRacine().isBlank()
                ? doc.causeRacine()
                : doc.equipmentCode();
        return new RetrievedDocumentDto(
                doc.interventionId(),
                doc.equipmentCode(),
                title,
                detail,
                round(doc.similarity() * 100.0)
        );
    }

    private static List<RetrievalStepDto> buildRetrievalSteps(AiDiagnosticTrace trace) {
        List<RetrievalStepDto> steps = new ArrayList<>();
        steps.add(new RetrievalStepDto("embedding", trace.embeddingStatus(), "Vectorisation de la requête"));
        steps.add(new RetrievalStepDto(
                "vector_search",
                "OK",
                trace.vectorResultCount() + " résultat(s)"
        ));
        if (trace.hybridSearchEnabled()) {
            steps.add(new RetrievalStepDto(
                    "hybrid_search",
                    "OK",
                    trace.textResultCount() + " résultat(s) texte, " + trace.mergedResultCount() + " après fusion RRF"
            ));
        }
        steps.add(new RetrievalStepDto(
                "filtering",
                "OK",
                trace.filteredCount() + " intervention(s) retenue(s)"
        ));
        return steps;
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
