package com.ocp.eia.presentation.controller;

import com.ocp.eia.application.dto.AiDto.AiAssistRequest;
import com.ocp.eia.application.dto.AiDto.AiAssistResponse;
import com.ocp.eia.modules.knowledge.application.RagAssistUseCase;
import com.ocp.eia.modules.knowledge.application.RagAssistStreamUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@ConditionalOnBean(RagAssistUseCase.class)
@PreAuthorize("hasAnyRole('TECHNICIEN', 'RESPONSABLE_EIA', 'ADMIN')")
@Validated
@Tag(name = "Assistant IA")
public class AiController {

    private final RagAssistUseCase ragAssistUseCase;
    private final RagAssistStreamUseCase ragAssistStreamUseCase;

    @PostMapping("/assist")
    @Operation(summary = "Assistance IA RAG pour diagnostic")
    public ResponseEntity<AiAssistResponse> assist(@Valid @RequestBody AiAssistRequest request) {
        return ResponseEntity.ok(ragAssistUseCase.assist(request));
    }

    @GetMapping(value = "/assist/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Assistance IA RAG streaming avec Server-Sent Events")
    public Flux<ServerSentEvent<String>> assistStream(
            @RequestParam @NotBlank @Size(max = 4000) String description,
            @RequestParam(required = false) UUID equipmentId,
            @RequestParam(required = false) UUID failureId,
            @RequestParam(defaultValue = "3") @Min(1) @Max(20) Integer topK) {

        AiAssistRequest request = new AiAssistRequest(failureId, equipmentId, description, topK);
        return ragAssistStreamUseCase.assistStream(request);
    }
}
