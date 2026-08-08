package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.InterventionDto.DocumentResponse;
import com.ocp.eia.application.mapper.InterventionMapper;
import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.model.InterventionDocument;
import com.ocp.eia.domain.repository.InterventionDocumentRepository;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.modules.maintenance.domain.port.DocumentStoragePort;
import com.ocp.eia.presentation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadDocumentUseCaseTest {

    @Mock
    private InterventionRepository interventionRepository;

    @Mock
    private InterventionDocumentRepository documentRepository;

    @Mock
    private DocumentStoragePort documentStorage;

    @Mock
    private InterventionMapper interventionMapper;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private UploadDocumentUseCase useCase;

    @Test
    void execute_storesFileAndPersistsDocument() {
        UUID interventionId = UUID.randomUUID();
        Intervention intervention = Intervention.builder().id(interventionId).build();
        DocumentResponse response = mock(DocumentResponse.class);

        when(interventionRepository.findById(interventionId)).thenReturn(Optional.of(intervention));
        when(file.getOriginalFilename()).thenReturn("rapport.pdf");
        when(documentStorage.store(interventionId, file)).thenReturn(
                new DocumentStoragePort.StoredDocument("stored.pdf", "/data/stored.pdf", "application/pdf", 1024L)
        );
        when(documentRepository.save(any(InterventionDocument.class))).thenAnswer(inv -> inv.getArgument(0));
        when(interventionMapper.toDocumentResponse(any(InterventionDocument.class))).thenReturn(response);

        assertSame(response, useCase.execute(interventionId, file));

        verify(documentStorage).store(interventionId, file);
        ArgumentCaptor<InterventionDocument> captor = ArgumentCaptor.forClass(InterventionDocument.class);
        verify(documentRepository).save(captor.capture());
        InterventionDocument saved = captor.getValue();
        assertEquals(intervention, saved.getIntervention());
        assertEquals("rapport.pdf", saved.getNomFichier());
        assertEquals("/data/stored.pdf", saved.getCheminStockage());
        assertEquals("application/pdf", saved.getTypeMime());
        assertEquals(1024L, saved.getTailleOctets());
    }

    @Test
    void execute_interventionNotFound_throws() {
        UUID interventionId = UUID.randomUUID();
        when(interventionRepository.findById(interventionId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(interventionId, file));
        verifyNoInteractions(documentStorage, documentRepository);
    }
}
