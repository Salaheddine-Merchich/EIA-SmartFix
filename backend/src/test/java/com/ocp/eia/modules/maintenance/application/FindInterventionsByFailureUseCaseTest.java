package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.InterventionDto.InterventionResponse;
import com.ocp.eia.application.mapper.InterventionMapper;
import com.ocp.eia.domain.model.Intervention;
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
class FindInterventionsByFailureUseCaseTest {

    @Mock
    private InterventionRepository interventionRepository;

    @Mock
    private InterventionMapper interventionMapper;

    @InjectMocks
    private FindInterventionsByFailureUseCase useCase;

    @Test
    void execute_pagesThenHydratesDetails() {
        UUID failureId = UUID.randomUUID();
        UUID interventionId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);
        Intervention intervention = Intervention.builder().id(interventionId).build();
        Intervention hydrated = Intervention.builder().id(interventionId).description("hydrated").build();
        Page<Intervention> page = new PageImpl<>(List.of(intervention), pageable, 1);
        InterventionResponse response = mock(InterventionResponse.class);

        when(interventionRepository.findByFailureId(failureId, pageable)).thenReturn(page);
        when(interventionRepository.findAllByIdWithDetails(List.of(interventionId))).thenReturn(List.of(hydrated));
        when(interventionMapper.toResponseList(List.of(hydrated))).thenReturn(List.of(response));

        var result = useCase.execute(failureId, pageable);

        assertEquals(1, result.content().size());
        assertEquals(response, result.content().get(0));
        assertEquals(1L, result.totalElements());
        assertEquals(1, result.totalPages());
        assertEquals(0, result.page());
        assertEquals(20, result.size());
        verify(interventionRepository).findByFailureId(failureId, pageable);
        verify(interventionRepository).findAllByIdWithDetails(List.of(interventionId));
    }

    @Test
    void execute_emptyPage_returnsEmptyContent() {
        UUID failureId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);
        Page<Intervention> page = Page.empty(pageable);

        when(interventionRepository.findByFailureId(failureId, pageable)).thenReturn(page);
        when(interventionMapper.toResponseList(List.of())).thenReturn(List.of());

        var result = useCase.execute(failureId, pageable);

        assertTrue(result.content().isEmpty());
        assertEquals(0L, result.totalElements());
        verify(interventionRepository, never()).findAllByIdWithDetails(any());
    }
}
