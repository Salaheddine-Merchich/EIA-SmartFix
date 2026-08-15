package com.ocp.eia.modules.knowledge.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocp.eia.application.dto.AiDto.AiAssistResponse;
import com.ocp.eia.application.dto.AiDto.AppendConversationMessagesRequest;
import com.ocp.eia.application.dto.AiDto.ConversationDetailDto;
import com.ocp.eia.application.dto.AiDto.ConversationMessageDto;
import com.ocp.eia.application.dto.AiDto.ConversationSummaryDto;
import com.ocp.eia.application.dto.AiDto.CreateConversationRequest;
import com.ocp.eia.domain.model.AiConversation;
import com.ocp.eia.domain.model.AiConversationMessage;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.domain.repository.AiConversationRepository;
import com.ocp.eia.infrastructure.security.SecurityUtils;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManageAiConversationsUseCase {

    private static final String DEFAULT_TITLE = "Nouvelle conversation";
    private static final int TITLE_MAX = 36;

    private final AiConversationRepository conversationRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ConversationSummaryDto> list() {
        User user = securityUtils.getCurrentUser();
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(user.getId()).stream()
                .map(c -> new ConversationSummaryDto(c.getId(), c.getTitle(), c.getUpdatedAt()))
                .toList();
    }

    @Transactional
    public ConversationDetailDto create(CreateConversationRequest request) {
        User user = securityUtils.getCurrentUser();
        String title = normalizeTitle(request != null ? request.title() : null, DEFAULT_TITLE);
        AiConversation conversation = AiConversation.builder()
                .user(user)
                .title(title)
                .build();
        conversation = conversationRepository.save(conversation);
        return toDetail(conversation);
    }

    @Transactional(readOnly = true)
    public ConversationDetailDto get(UUID id) {
        return toDetail(ownedConversationWithMessages(id));
    }

    @Transactional
    public ConversationDetailDto append(UUID id, AppendConversationMessagesRequest request) {
        AiConversation conversation = ownedConversationWithMessages(id);
        if (conversation.getMessages().isEmpty()) {
            conversation.setTitle(normalizeTitle(request.userContent(), conversation.getTitle()));
        }

        AiConversationMessage userMessage = AiConversationMessage.builder()
                .conversation(conversation)
                .role("user")
                .content(request.userContent())
                .build();
        conversation.getMessages().add(userMessage);

        JsonNode payloadJson = toJson(request.assistantResponse());
        String assistantContent = assistantContent(request.assistantResponse());
        AiConversationMessage assistantMessage = AiConversationMessage.builder()
                .conversation(conversation)
                .role("assistant")
                .content(assistantContent)
                .payload(payloadJson)
                .build();
        conversation.getMessages().add(assistantMessage);

        conversation = conversationRepository.save(conversation);
        return toDetail(conversation);
    }

    @Transactional
    public void delete(UUID id) {
        AiConversation conversation = ownedConversation(id);
        conversationRepository.delete(conversation);
    }

    @Transactional
    public void deleteAll() {
        User user = securityUtils.getCurrentUser();
        conversationRepository.deleteByUserId(user.getId());
    }

    private AiConversation ownedConversation(UUID id) {
        User user = securityUtils.getCurrentUser();
        return conversationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation introuvable"));
    }

    private AiConversation ownedConversationWithMessages(UUID id) {
        User user = securityUtils.getCurrentUser();
        return conversationRepository.findByIdAndUserIdWithMessages(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation introuvable"));
    }

    private ConversationDetailDto toDetail(AiConversation conversation) {
        List<ConversationMessageDto> messages = conversation.getMessages().stream()
                .map(this::toMessageDto)
                .toList();
        return new ConversationDetailDto(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messages
        );
    }

    private ConversationMessageDto toMessageDto(AiConversationMessage message) {
        return new ConversationMessageDto(
                message.getId(),
                message.getRole(),
                message.getContent(),
                fromJson(message.getPayload()),
                message.getCreatedAt()
        );
    }

    private String normalizeTitle(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String trimmed = raw.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= TITLE_MAX) {
            return trimmed;
        }
        String slice = trimmed.substring(0, TITLE_MAX);
        int lastSpace = slice.lastIndexOf(' ');
        return lastSpace > 16 ? slice.substring(0, lastSpace) : slice;
    }

    private String assistantContent(AiAssistResponse response) {
        if (response == null || response.suggestions() == null) {
            return "";
        }
        if (response.suggestions().summary() != null && !response.suggestions().summary().isBlank()) {
            return response.suggestions().summary();
        }
        if (response.suggestions().advice() != null) {
            return response.suggestions().advice();
        }
        return "";
    }

    private JsonNode toJson(AiAssistResponse response) {
        if (response == null) {
            return null;
        }
        return objectMapper.valueToTree(response);
    }

    private AiAssistResponse fromJson(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            return null;
        }
        return objectMapper.convertValue(payload, AiAssistResponse.class);
    }
}
