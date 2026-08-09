package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.FailureDto.FailureResponse;
import com.ocp.eia.application.mapper.FailureMapper;
import com.ocp.eia.domain.model.Failure;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
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
class FindFailureByIdUseCaseTest {

    @Mock private FailureRepository failureRepository;
    @Mock private FailureMapper failureMapper;

    @InjectMocks private FindFailureByIdUseCase useCase;

    @Test
    void execute_returnsMappedResponse() {
        UUID id = UUID.randomUUID();
        Failure failure = Failure.builder().id(id).build();
        FailureResponse response = mock(FailureResponse.class);

        when(failureRepository.findByIdWithDetails(id)).thenReturn(Optional.of(failure));
        when(failureMapper.toResponse(failure)).thenReturn(response);

        assertSame(response, useCase.execute(id));
    }

    @Test
    void execute_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(failureRepository.findByIdWithDetails(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(id));
    }
}
