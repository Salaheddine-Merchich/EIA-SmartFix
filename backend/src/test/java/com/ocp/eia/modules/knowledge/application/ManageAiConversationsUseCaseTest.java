package com.ocp.eia.modules.knowledge.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocp.eia.application.dto.AiDto.AiAssistResponse;
import com.ocp.eia.application.dto.AiDto.AiSuggestions;
import com.ocp.eia.application.dto.AiDto.AppendConversationMessagesRequest;
import com.ocp.eia.application.dto.AiDto.CreateConversationRequest;
import com.ocp.eia.domain.model.AiConversation;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.domain.repository.AiConversationRepository;
import com.ocp.eia.infrastructure.security.SecurityUtils;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageAiConversationsUseCaseTest {

    @Mock private AiConversationRepository conversationRepository;
    @Mock private SecurityUtils securityUtils;

    private ManageAiConversationsUseCase useCase;
    private User owner;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(UUID.randomUUID())
                .email("tech@ocp.ma")
                .nomPrenom("Tech")
                .build();
        when(securityUtils.getCurrentUser()).thenReturn(owner);
        useCase = new ManageAiConversationsUseCase(conversationRepository, securityUtils, new ObjectMapper());
    }

    @Test
    void list_returnsOnlyCurrentUserConversations() {
        UUID id = UUID.randomUUID();
        AiConversation conversation = AiConversation.builder()
                .id(id)
                .user(owner)
                .title("Pompe PV")
                .updatedAt(Instant.parse("2026-08-14T10:00:00Z"))
                .build();
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(owner.getId()))
                .thenReturn(List.of(conversation));

        var result = useCase.list();

        assertEquals(1, result.size());
        assertEquals(id, result.getFirst().id());
        assertEquals("Pompe PV", result.getFirst().title());
    }

    @Test
    void get_otherUser_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(conversationRepository.findByIdAndUserIdWithMessages(id, owner.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.get(id));
    }

    @Test
    void delete_otherUser_doesNotDelete() {
        UUID id = UUID.randomUUID();
        when(conversationRepository.findByIdAndUserId(id, owner.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.delete(id));
        verify(conversationRepository, never()).delete(any());
    }

    @Test
    void append_setsTitleFromFirstUserMessage() {
        UUID id = UUID.randomUUID();
        AiConversation conversation = AiConversation.builder()
                .id(id)
                .user(owner)
                .title("Nouvelle conversation")
                .messages(new ArrayList<>())
                .build();
        when(conversationRepository.findByIdAndUserIdWithMessages(id, owner.getId()))
                .thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(AiConversation.class))).thenAnswer(inv -> inv.getArgument(0));

        AiAssistResponse response = new AiAssistResponse(
                List.of(),
                new AiSuggestions(List.of("Cause"), List.of("Action"), "Résumé", "Conseil"),
                "disclaimer",
                List.of(),
                null
        );

        var detail = useCase.append(id, new AppendConversationMessagesRequest(
                "Defaut E21 variateur convoyeur Hitachi SJ200",
                response
        ));

        assertEquals("Defaut E21 variateur convoyeur Hitachi SJ200", detail.title());
        assertEquals(2, detail.messages().size());
        assertEquals("user", detail.messages().getFirst().role());
        assertEquals("assistant", detail.messages().get(1).role());
    }

    @Test
    void deleteAll_scopesToCurrentUser() {
        useCase.deleteAll();
        verify(conversationRepository).deleteByUserId(owner.getId());
    }

    @Test
    void create_usesDefaultTitleWhenBlank() {
        when(conversationRepository.save(any(AiConversation.class))).thenAnswer(inv -> {
            AiConversation saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        var created = useCase.create(new CreateConversationRequest("  "));
        assertEquals("Nouvelle conversation", created.title());

        ArgumentCaptor<AiConversation> captor = ArgumentCaptor.forClass(AiConversation.class);
        verify(conversationRepository).save(captor.capture());
        assertTrue(captor.getValue().getUser().getId().equals(owner.getId()));
    }
}
