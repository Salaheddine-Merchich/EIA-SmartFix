package com.ocp.eia.presentation.controller;

import com.ocp.eia.application.dto.AiDto.AppendConversationMessagesRequest;
import com.ocp.eia.application.dto.AiDto.ConversationDetailDto;
import com.ocp.eia.application.dto.AiDto.ConversationSummaryDto;
import com.ocp.eia.application.dto.AiDto.CreateConversationRequest;
import com.ocp.eia.modules.knowledge.application.ManageAiConversationsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/conversations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TECHNICIEN', 'RESPONSABLE_EIA', 'ADMIN')")
@Tag(name = "Conversations IA")
public class AiConversationController {

    private final ManageAiConversationsUseCase conversationsUseCase;

    @GetMapping
    @Operation(summary = "Lister les conversations de l'utilisateur connecté")
    public List<ConversationSummaryDto> list() {
        return conversationsUseCase.list();
    }

    @PostMapping
    @Operation(summary = "Créer une conversation vide")
    public ResponseEntity<ConversationDetailDto> create(@RequestBody(required = false) @Valid CreateConversationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(conversationsUseCase.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Charger une conversation")
    public ConversationDetailDto get(@PathVariable UUID id) {
        return conversationsUseCase.get(id);
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Ajouter un tour user + assistant")
    public ConversationDetailDto append(
            @PathVariable UUID id,
            @Valid @RequestBody AppendConversationMessagesRequest request
    ) {
        return conversationsUseCase.append(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une conversation")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        conversationsUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Supprimer tout l'historique de l'utilisateur")
    public ResponseEntity<Void> deleteAll() {
        conversationsUseCase.deleteAll();
        return ResponseEntity.noContent().build();
    }
}
