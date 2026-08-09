package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.InterventionDto.DocumentResponse;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.modules.maintenance.domain.port.DocumentStoragePort;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadDocumentUseCaseTest {

    @Mock
    private InterventionRepository interventionRepository;

    @Mock
    private DocumentStoragePort documentStorage;

    @Mock
    private InterventionDocumentWriter documentWriter;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private UploadDocumentUseCase useCase;

    @Test
    void execute_storesFileAndPersistsDocument() {
        UUID interventionId = UUID.randomUUID();
        DocumentResponse response = mock(DocumentResponse.class);
        DocumentStoragePort.StoredDocument stored =
                new DocumentStoragePort.StoredDocument("stored.pdf", "/data/stored.pdf", "application/pdf", 1024L);

        when(interventionRepository.existsById(interventionId)).thenReturn(true);
        when(documentStorage.store(interventionId, file)).thenReturn(stored);
        when(documentWriter.saveMetadata(interventionId, file, stored)).thenReturn(response);

        assertSame(response, useCase.execute(interventionId, file));

        verify(documentStorage).store(interventionId, file);
        verify(documentWriter).saveMetadata(interventionId, file, stored);
        verify(documentStorage, never()).delete(any());
    }

    @Test
    void execute_interventionNotFound_throws() {
        UUID interventionId = UUID.randomUUID();
        when(interventionRepository.existsById(interventionId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(interventionId, file));
        verifyNoInteractions(documentStorage, documentWriter);
    }

    @Test
    void execute_dbFailure_deletesStoredFile() {
        UUID interventionId = UUID.randomUUID();
        DocumentStoragePort.StoredDocument stored =
                new DocumentStoragePort.StoredDocument("stored.pdf", "/data/stored.pdf", "application/pdf", 1024L);

        when(interventionRepository.existsById(interventionId)).thenReturn(true);
        when(documentStorage.store(interventionId, file)).thenReturn(stored);
        when(documentWriter.saveMetadata(eq(interventionId), eq(file), eq(stored)))
                .thenThrow(new RuntimeException("DB down"));

        assertThrows(RuntimeException.class, () -> useCase.execute(interventionId, file));
        verify(documentStorage).delete("/data/stored.pdf");
    }
}
