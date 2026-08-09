package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.InterventionDto.DocumentResponse;
import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.model.Role;
import com.ocp.eia.domain.model.StatutValidation;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.infrastructure.security.SecurityUtils;
import com.ocp.eia.modules.maintenance.domain.port.DocumentStoragePort;
import com.ocp.eia.shared.exception.DomainRuleViolationException;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadDocumentUseCaseTest {

    @Mock private InterventionRepository interventionRepository;
    @Mock private DocumentStoragePort documentStorage;
    @Mock private InterventionDocumentWriter documentWriter;
    @Mock private SecurityUtils securityUtils;
    @Mock private MultipartFile file;
    @InjectMocks private UploadDocumentUseCase useCase;

    @Test
    void execute_storesFileAndPersistsDocument() {
        UUID interventionId = UUID.randomUUID();
        UUID techId = UUID.randomUUID();
        Intervention intervention = intervention(interventionId, techId, StatutValidation.BROUILLON);
        User current = user(techId, Role.TECHNICIEN);
        DocumentResponse response = mock(DocumentResponse.class);
        DocumentStoragePort.StoredDocument stored =
                new DocumentStoragePort.StoredDocument("stored.pdf", "/data/stored.pdf", "application/pdf", 1024L);

        when(interventionRepository.findByIdWithDetails(interventionId)).thenReturn(Optional.of(intervention));
        when(securityUtils.getCurrentUser()).thenReturn(current);
        when(documentStorage.store(interventionId, file)).thenReturn(stored);
        when(documentWriter.saveMetadata(interventionId, file, stored)).thenReturn(response);

        assertSame(response, useCase.execute(interventionId, file));

        verify(documentStorage).store(interventionId, file);
        verify(documentWriter).saveMetadata(interventionId, file, stored);
        verify(documentStorage, never()).delete(any());
    }

    @Test
    void execute_validee_forbidden() {
        UUID interventionId = UUID.randomUUID();
        UUID techId = UUID.randomUUID();
        when(interventionRepository.findByIdWithDetails(interventionId))
                .thenReturn(Optional.of(intervention(interventionId, techId, StatutValidation.VALIDEE)));
        when(securityUtils.getCurrentUser()).thenReturn(user(techId, Role.TECHNICIEN));

        assertThrows(DomainRuleViolationException.class, () -> useCase.execute(interventionId, file));
        verifyNoInteractions(documentStorage, documentWriter);
    }

    @Test
    void execute_interventionNotFound_throws() {
        UUID interventionId = UUID.randomUUID();
        when(interventionRepository.findByIdWithDetails(interventionId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(interventionId, file));
        verifyNoInteractions(documentStorage, documentWriter);
    }

    @Test
    void execute_dbFailure_deletesStoredFile() {
        UUID interventionId = UUID.randomUUID();
        UUID techId = UUID.randomUUID();
        DocumentStoragePort.StoredDocument stored =
                new DocumentStoragePort.StoredDocument("stored.pdf", "/data/stored.pdf", "application/pdf", 1024L);

        when(interventionRepository.findByIdWithDetails(interventionId))
                .thenReturn(Optional.of(intervention(interventionId, techId, StatutValidation.BROUILLON)));
        when(securityUtils.getCurrentUser()).thenReturn(user(techId, Role.TECHNICIEN));
        when(documentStorage.store(interventionId, file)).thenReturn(stored);
        when(documentWriter.saveMetadata(eq(interventionId), eq(file), eq(stored)))
                .thenThrow(new RuntimeException("DB down"));

        assertThrows(RuntimeException.class, () -> useCase.execute(interventionId, file));
        verify(documentStorage).delete("/data/stored.pdf");
    }

    private static Intervention intervention(UUID id, UUID techId, StatutValidation statut) {
        return Intervention.builder()
                .id(id)
                .statutValidation(statut)
                .technicien(User.builder().id(techId).role(Role.TECHNICIEN).build())
                .build();
    }

    private static User user(UUID id, Role role) {
        return User.builder().id(id).role(role).build();
    }
}
