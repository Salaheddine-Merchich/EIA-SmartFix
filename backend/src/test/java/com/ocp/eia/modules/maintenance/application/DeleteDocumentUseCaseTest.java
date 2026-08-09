package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.domain.model.InterventionDocument;
import com.ocp.eia.domain.repository.InterventionDocumentRepository;
import com.ocp.eia.modules.maintenance.domain.port.DocumentStoragePort;
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

    @Mock
    private InterventionDocumentRepository documentRepository;

    @Mock
    private DocumentStoragePort documentStorage;

    @InjectMocks
    private DeleteDocumentUseCase useCase;

    @Test
    void execute_deletesFileThenMetadata() {
        UUID interventionId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        InterventionDocument document = InterventionDocument.builder()
                .id(documentId)
                .cheminStockage("/data/rapport.pdf")
                .build();

        when(documentRepository.findByIdAndInterventionId(documentId, interventionId))
                .thenReturn(Optional.of(document));

        useCase.execute(interventionId, documentId);

        InOrder inOrder = inOrder(documentStorage, documentRepository);
        inOrder.verify(documentStorage).delete("/data/rapport.pdf");
        inOrder.verify(documentRepository).delete(document);
    }

    @Test
    void execute_notFound_throws() {
        UUID interventionId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        when(documentRepository.findByIdAndInterventionId(documentId, interventionId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(interventionId, documentId));
        verifyNoInteractions(documentStorage);
        verify(documentRepository, never()).delete(any());
    }
}
