package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.model.InterventionDocument;
import com.ocp.eia.domain.model.Role;
import com.ocp.eia.domain.model.StatutValidation;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.domain.repository.InterventionDocumentRepository;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.infrastructure.security.SecurityUtils;
import com.ocp.eia.modules.maintenance.domain.port.DocumentStoragePort;
import com.ocp.eia.shared.exception.DomainRuleViolationException;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteDocumentUseCaseTest {

    @Mock private InterventionDocumentRepository documentRepository;
    @Mock private InterventionRepository interventionRepository;
    @Mock private DocumentStoragePort documentStorage;
    @Mock private SecurityUtils securityUtils;
    @InjectMocks private DeleteDocumentUseCase useCase;

    @Test
    void execute_deletesFileThenMetadata() {
        UUID interventionId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID techId = UUID.randomUUID();
        Intervention intervention = Intervention.builder()
                .id(interventionId)
                .statutValidation(StatutValidation.BROUILLON)
                .technicien(User.builder().id(techId).role(Role.TECHNICIEN).build())
                .build();
        InterventionDocument document = InterventionDocument.builder()
                .id(documentId)
                .cheminStockage("/data/rapport.pdf")
                .build();

        when(interventionRepository.findByIdWithDetails(interventionId)).thenReturn(Optional.of(intervention));
        when(securityUtils.getCurrentUser()).thenReturn(User.builder().id(techId).role(Role.TECHNICIEN).build());
        when(documentRepository.findByIdAndInterventionId(documentId, interventionId))
                .thenReturn(Optional.of(document));

        useCase.execute(interventionId, documentId);

        InOrder inOrder = inOrder(documentStorage, documentRepository);
        inOrder.verify(documentStorage).delete("/data/rapport.pdf");
        inOrder.verify(documentRepository).delete(document);
    }

    @Test
    void execute_validee_forbidden() {
        UUID interventionId = UUID.randomUUID();
        UUID techId = UUID.randomUUID();
        when(interventionRepository.findByIdWithDetails(interventionId)).thenReturn(Optional.of(
                Intervention.builder()
                        .id(interventionId)
                        .statutValidation(StatutValidation.VALIDEE)
                        .technicien(User.builder().id(techId).role(Role.TECHNICIEN).build())
                        .build()));
        when(securityUtils.getCurrentUser()).thenReturn(User.builder().id(techId).role(Role.TECHNICIEN).build());

        assertThrows(DomainRuleViolationException.class,
                () -> useCase.execute(interventionId, UUID.randomUUID()));
        verifyNoInteractions(documentStorage);
    }

    @Test
    void execute_notFound_throws() {
        UUID interventionId = UUID.randomUUID();
        when(interventionRepository.findByIdWithDetails(interventionId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(interventionId, UUID.randomUUID()));
        verifyNoInteractions(documentStorage);
        verify(documentRepository, never()).delete(any());
    }
}
