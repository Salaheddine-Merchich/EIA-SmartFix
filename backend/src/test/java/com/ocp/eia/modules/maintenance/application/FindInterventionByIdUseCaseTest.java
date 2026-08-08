package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.InterventionDto.InterventionResponse;
import com.ocp.eia.application.mapper.InterventionMapper;
import com.ocp.eia.domain.model.*;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.presentation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindInterventionByIdUseCaseTest {

    @Mock
    private InterventionRepository interventionRepository;

    @Mock
    private InterventionMapper interventionMapper;

    @InjectMocks
    private FindInterventionByIdUseCase useCase;

    @Test
    void execute_returnsMappedResponse() {
        UUID id = UUID.randomUUID();
        Intervention intervention = intervention(id);
        InterventionResponse response = mock(InterventionResponse.class);

        when(interventionRepository.findByIdWithDetails(id)).thenReturn(Optional.of(intervention));
        when(interventionMapper.toResponse(intervention)).thenReturn(response);

        assertSame(response, useCase.execute(id));
    }

    @Test
    void execute_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(interventionRepository.findByIdWithDetails(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(id));
        verifyNoInteractions(interventionMapper);
    }

    private Intervention intervention(UUID id) {
        User technicien = User.builder().id(UUID.randomUUID()).role(Role.TECHNICIEN).build();
        Equipment equipment = Equipment.builder().id(UUID.randomUUID()).code("EQ-001").build();
        Failure failure = Failure.builder().id(UUID.randomUUID()).equipment(equipment).build();
        return Intervention.builder()
                .id(id)
                .statutValidation(StatutValidation.BROUILLON)
                .technicien(technicien)
                .failure(failure)
                .build();
    }
}
