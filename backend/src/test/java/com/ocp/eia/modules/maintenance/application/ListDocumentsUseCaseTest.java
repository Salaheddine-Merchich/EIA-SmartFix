package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.InterventionDto.DocumentResponse;
import com.ocp.eia.application.mapper.InterventionMapper;
import com.ocp.eia.domain.model.InterventionDocument;
import com.ocp.eia.domain.repository.InterventionDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListDocumentsUseCaseTest {

    @Mock
    private InterventionDocumentRepository documentRepository;

    @Mock
    private InterventionMapper interventionMapper;

    @InjectMocks
    private ListDocumentsUseCase useCase;

    @Test
    void execute_returnsMappedDocuments() {
        UUID interventionId = UUID.randomUUID();
        InterventionDocument doc1 = InterventionDocument.builder().id(UUID.randomUUID()).build();
        InterventionDocument doc2 = InterventionDocument.builder().id(UUID.randomUUID()).build();
        DocumentResponse response1 = mock(DocumentResponse.class);
        DocumentResponse response2 = mock(DocumentResponse.class);

        when(documentRepository.findByInterventionId(interventionId)).thenReturn(List.of(doc1, doc2));
        when(interventionMapper.toDocumentResponse(doc1)).thenReturn(response1);
        when(interventionMapper.toDocumentResponse(doc2)).thenReturn(response2);

        List<DocumentResponse> result = useCase.execute(interventionId);

        assertEquals(2, result.size());
        assertEquals(response1, result.get(0));
        assertEquals(response2, result.get(1));
    }

    @Test
    void execute_emptyList_returnsEmpty() {
        UUID interventionId = UUID.randomUUID();
        when(documentRepository.findByInterventionId(interventionId)).thenReturn(List.of());

        assertEquals(0, useCase.execute(interventionId).size());
    }
}
