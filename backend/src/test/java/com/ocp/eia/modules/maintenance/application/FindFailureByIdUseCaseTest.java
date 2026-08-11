package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.FailureDto.FailureResponse;
import com.ocp.eia.application.mapper.FailureMapper;
import com.ocp.eia.domain.model.Criticite;
import com.ocp.eia.domain.model.Failure;
import com.ocp.eia.domain.model.StatutPanne;
import com.ocp.eia.domain.model.StatutValidation;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindFailureByIdUseCaseTest {

    @Mock private FailureRepository failureRepository;
    @Mock private InterventionRepository interventionRepository;
    @Mock private FailureMapper failureMapper;

    @InjectMocks private FindFailureByIdUseCase useCase;

    @Test
    void execute_returnsResponseWithInterventionStats() {
        UUID id = UUID.randomUUID();
        Failure failure = Failure.builder().id(id).build();
        FailureResponse base = new FailureResponse(
                id, UUID.randomUUID(), "EQ-1", "Pompe", Instant.now(),
                Criticite.MOYENNE, "Zone A", UUID.randomUUID(), "Tech", null, null,
                StatutPanne.OUVERTE, "Desc", "F001", 0, null
        );

        when(failureRepository.findByIdWithDetails(id)).thenReturn(Optional.of(failure));
        when(failureMapper.toResponseWithoutInterventionStats(failure)).thenReturn(base);
        List<Object[]> countRows = new ArrayList<>();
        countRows.add(new Object[]{id, 2L});
        List<Object[]> statutRows = new ArrayList<>();
        statutRows.add(new Object[]{id, StatutValidation.VALIDEE});
        when(interventionRepository.countByFailureIds(List.of(id))).thenReturn(countRows);
        when(interventionRepository.findLatestStatutByFailureIds(List.of(id))).thenReturn(statutRows);

        FailureResponse response = useCase.execute(id);

        assertEquals(2, response.interventionCount());
        assertEquals(StatutValidation.VALIDEE, response.latestInterventionStatut());
    }

    @Test
    void execute_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(failureRepository.findByIdWithDetails(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(id));
    }
}
