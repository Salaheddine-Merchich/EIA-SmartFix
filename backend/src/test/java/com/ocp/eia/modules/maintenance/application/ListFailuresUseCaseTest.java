package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.FailureDto.FailureResponse;
import com.ocp.eia.application.mapper.FailureMapper;
import com.ocp.eia.domain.model.Failure;
import com.ocp.eia.domain.model.StatutValidation;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.domain.repository.InterventionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListFailuresUseCaseTest {

    @Mock private FailureRepository failureRepository;
    @Mock private InterventionRepository interventionRepository;
    @Mock private FailureMapper failureMapper;

    @InjectMocks private ListFailuresUseCase useCase;

    @Test
    void execute_returnsPagedResponseWithBatchStats() {
        UUID equipmentId = UUID.randomUUID();
        UUID failureId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);
        Failure failure = Failure.builder().id(failureId).build();
        Page<Failure> page = new PageImpl<>(List.of(failure), pageable, 1);
        FailureResponse base = new FailureResponse(
                failureId, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                0, null
        );

        when(failureRepository.search(equipmentId, null, null, null, null, pageable)).thenReturn(page);
        when(failureMapper.toResponseWithoutInterventionStats(failure)).thenReturn(base);
        when(interventionRepository.countByFailureIds(List.of(failureId)))
                .thenReturn(List.<Object[]>of(new Object[]{failureId, 3L}));
        when(interventionRepository.findLatestStatutByFailureIds(List.of(failureId)))
                .thenReturn(List.<Object[]>of(new Object[]{failureId, StatutValidation.SOUMISE}));

        var result = useCase.execute(equipmentId, null, null, null, null, pageable);

        assertEquals(1, result.content().size());
        assertEquals(3, result.content().get(0).interventionCount());
        assertEquals(StatutValidation.SOUMISE, result.content().get(0).latestInterventionStatut());
        assertEquals(1L, result.totalElements());
        verify(failureMapper, never()).toResponseList(any());
    }

    @Test
    void execute_emptyPage_skipsBatchQueries() {
        UUID equipmentId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);
        Page<Failure> page = Page.empty(pageable);

        when(failureRepository.search(equipmentId, null, null, null, null, pageable)).thenReturn(page);

        var result = useCase.execute(equipmentId, null, null, null, null, pageable);

        assertTrue(result.content().isEmpty());
        verify(interventionRepository, never()).countByFailureIds(any());
        verify(interventionRepository, never()).findLatestStatutByFailureIds(any());
    }
}
