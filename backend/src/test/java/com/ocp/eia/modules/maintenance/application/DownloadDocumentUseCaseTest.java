package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.domain.model.InterventionDocument;
import com.ocp.eia.domain.repository.InterventionDocumentRepository;
import com.ocp.eia.modules.maintenance.domain.port.DocumentStoragePort;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DownloadDocumentUseCaseTest {

    @Mock
    private InterventionDocumentRepository documentRepository;

    @Mock
    private DocumentStoragePort documentStorage;

    @Mock
    private Resource resource;

    @InjectMocks
    private DownloadDocumentUseCase useCase;

    @Test
    void execute_documentExists_returnsDownloadResultWithContentType() {
        // Given
        UUID interventionId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        String filename = "rapport.pdf";
        String contentType = "application/pdf";
        String storagePath = "/data/documents/intervention-id/uuid_rapport.pdf";

        InterventionDocument document = InterventionDocument.builder()
                .id(documentId)
                .nomFichier(filename)
                .cheminStockage(storagePath)
                .typeMime(contentType)
                .tailleOctets(1024L)
                .build();

        when(documentRepository.findByIdAndInterventionId(documentId, interventionId))
                .thenReturn(Optional.of(document));
        when(documentStorage.load(storagePath)).thenReturn(resource);

        // When
        DownloadDocumentUseCase.DownloadResult result = useCase.execute(interventionId, documentId);

        // Then
        assertNotNull(result);
        assertEquals(resource, result.resource());
        assertEquals(filename, result.filename());
        assertEquals(contentType, result.contentType());
        
        verify(documentRepository).findByIdAndInterventionId(documentId, interventionId);
        verify(documentStorage).load(storagePath);
    }

    @Test
    void execute_documentExistsWithDocxMimeType_returnsCorrectContentType() {
        // Given
        UUID interventionId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        String filename = "document.docx";
        String contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        String storagePath = "/data/documents/intervention-id/uuid_document.docx";

        InterventionDocument document = InterventionDocument.builder()
                .id(documentId)
                .nomFichier(filename)
                .cheminStockage(storagePath)
                .typeMime(contentType)
                .tailleOctets(2048L)
                .build();

        when(documentRepository.findByIdAndInterventionId(documentId, interventionId))
                .thenReturn(Optional.of(document));
        when(documentStorage.load(storagePath)).thenReturn(resource);

        // When
        DownloadDocumentUseCase.DownloadResult result = useCase.execute(interventionId, documentId);

        // Then
        assertEquals(contentType, result.contentType());
    }

    @Test
    void execute_documentExistsWithOctetStreamMimeType_returnsOctetStream() {
        // Given
        UUID interventionId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        String filename = "unknown.bin";
        String contentType = "application/octet-stream";
        String storagePath = "/data/documents/intervention-id/uuid_unknown.bin";

        InterventionDocument document = InterventionDocument.builder()
                .id(documentId)
                .nomFichier(filename)
                .cheminStockage(storagePath)
                .typeMime(contentType)
                .tailleOctets(512L)
                .build();

        when(documentRepository.findByIdAndInterventionId(documentId, interventionId))
                .thenReturn(Optional.of(document));
        when(documentStorage.load(storagePath)).thenReturn(resource);

        // When
        DownloadDocumentUseCase.DownloadResult result = useCase.execute(interventionId, documentId);

        // Then
        assertEquals(contentType, result.contentType());
    }

    @Test
    void execute_documentNotFound_throwsResourceNotFoundException() {
        // Given
        UUID interventionId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        when(documentRepository.findByIdAndInterventionId(documentId, interventionId))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> useCase.execute(interventionId, documentId)
        );

        assertEquals("Document introuvable: " + documentId, exception.getMessage());
        verify(documentRepository).findByIdAndInterventionId(documentId, interventionId);
        verifyNoInteractions(documentStorage);
    }

    @Test
    void execute_documentExistsWithNullMimeType_returnsNullContentType() {
        // Given
        UUID interventionId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        String filename = "document.txt";
        String storagePath = "/data/documents/intervention-id/uuid_document.txt";

        InterventionDocument document = InterventionDocument.builder()
                .id(documentId)
                .nomFichier(filename)
                .cheminStockage(storagePath)
                .typeMime(null) // MIME type is null
                .tailleOctets(256L)
                .build();

        when(documentRepository.findByIdAndInterventionId(documentId, interventionId))
                .thenReturn(Optional.of(document));
        when(documentStorage.load(storagePath)).thenReturn(resource);

        // When
        DownloadDocumentUseCase.DownloadResult result = useCase.execute(interventionId, documentId);

        // Then
        assertNull(result.contentType());
    }
}