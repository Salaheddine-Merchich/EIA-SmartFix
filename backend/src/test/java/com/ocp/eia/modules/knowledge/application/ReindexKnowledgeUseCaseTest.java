package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.application.dto.KnowledgeDto.ReindexResponse;
import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.model.StatutValidation;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.modules.knowledge.application.IndexInterventionUseCase.IndexOutcome;
import com.ocp.eia.modules.maintenance.application.event.InterventionKnowledgePayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReindexKnowledgeUseCaseTest {

    @Mock private InterventionRepository interventionRepository;
    @Mock private IndexInterventionUseCase indexInterventionUseCase;

    @InjectMocks private ReindexKnowledgeUseCase useCase;

    @Test
    void execute_indexesAllValidatedInterventions() {
        Intervention i1 = Intervention.builder().id(UUID.randomUUID()).build();
        Intervention i2 = Intervention.builder().id(UUID.randomUUID()).build();
        when(interventionRepository.findByStatutValidationWithDetails(StatutValidation.VALIDEE))
                .thenReturn(List.of(i1, i2));
        when(indexInterventionUseCase.index(any(InterventionKnowledgePayload.class)))
                .thenReturn(IndexOutcome.INDEXED);

        ReindexResponse response = useCase.execute();

        assertEquals(2, response.processed());
        assertEquals(2, response.indexed());
        assertEquals(0, response.skipped());
        assertEquals(0, response.errors());
        verify(indexInterventionUseCase, times(2)).index(any(InterventionKnowledgePayload.class));
    }

    @Test
    void execute_failureOnOneIntervention_continuesLoopAndCountsError() {
        Intervention i1 = Intervention.builder().id(UUID.randomUUID()).build();
        Intervention i2 = Intervention.builder().id(UUID.randomUUID()).build();
        Intervention i3 = Intervention.builder().id(UUID.randomUUID()).build();
        when(interventionRepository.findByStatutValidationWithDetails(StatutValidation.VALIDEE))
                .thenReturn(List.of(i1, i2, i3));
        when(indexInterventionUseCase.index(any(InterventionKnowledgePayload.class)))
                .thenReturn(IndexOutcome.FAILED, IndexOutcome.INDEXED, IndexOutcome.SKIPPED);

        ReindexResponse response = useCase.execute();

        assertEquals(3, response.processed());
        assertEquals(1, response.indexed());
        assertEquals(1, response.skipped());
        assertEquals(1, response.errors());
        verify(indexInterventionUseCase, times(3)).index(any(InterventionKnowledgePayload.class));
    }

    @Test
    void execute_returnsCorrectSummary() {
        when(interventionRepository.findByStatutValidationWithDetails(StatutValidation.VALIDEE))
                .thenReturn(List.of(
                        Intervention.builder().id(UUID.randomUUID()).build(),
                        Intervention.builder().id(UUID.randomUUID()).build(),
                        Intervention.builder().id(UUID.randomUUID()).build(),
                        Intervention.builder().id(UUID.randomUUID()).build()
                ));
        when(indexInterventionUseCase.index(any(InterventionKnowledgePayload.class)))
                .thenReturn(IndexOutcome.INDEXED, IndexOutcome.SKIPPED, IndexOutcome.FAILED, IndexOutcome.INDEXED);

        ReindexResponse response = useCase.execute();

        assertEquals(4, response.processed());
        assertEquals(2, response.indexed());
        assertEquals(1, response.skipped());
        assertEquals(1, response.errors());
    }

    @Test
    void execute_noValidatedInterventions_returnsZeroSummary() {
        when(interventionRepository.findByStatutValidationWithDetails(StatutValidation.VALIDEE))
                .thenReturn(List.of());

        ReindexResponse response = useCase.execute();

        assertEquals(0, response.processed());
        assertEquals(0, response.indexed());
        verifyNoInteractions(indexInterventionUseCase);
    }
}
