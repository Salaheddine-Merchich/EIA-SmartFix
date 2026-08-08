package com.ocp.eia.presentation.controller;

import com.ocp.eia.application.dto.KnowledgeDto.ReindexResponse;
import com.ocp.eia.modules.knowledge.application.IndexKnowledgeDocumentUseCase;
import com.ocp.eia.modules.knowledge.application.ReindexKnowledgeUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/knowledge")
@RequiredArgsConstructor
@ConditionalOnBean(ReindexKnowledgeUseCase.class)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration Knowledge")
public class KnowledgeAdminController {

    private final ReindexKnowledgeUseCase reindexKnowledgeUseCase;
    private final IndexKnowledgeDocumentUseCase indexKnowledgeDocumentUseCase;

    @PostMapping("/reindex")
    @Operation(summary = "Réindexer entièrement la base de connaissances RAG")
    public ResponseEntity<ReindexResponse> reindex() {
        return ResponseEntity.ok(reindexKnowledgeUseCase.execute());
    }
    
    @PostMapping("/reindex-documents")
    @Operation(summary = "Réindexer les documents techniques pour la recherche vectorielle")
    public ResponseEntity<Object> reindexDocuments() {
        IndexKnowledgeDocumentUseCase.IndexResults results = indexKnowledgeDocumentUseCase.indexAllDocuments();
        
        return ResponseEntity.ok(Map.of(
            "message", "Indexation des documents terminée",
            "indexed", results.indexed(),
            "skipped", results.skipped(),
            "failed", results.failed(),
            "total", results.total()
        ));
    }
}
