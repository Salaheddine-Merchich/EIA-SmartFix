package com.ocp.eia.modules.maintenance.application;

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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteFailureUseCaseTest {

    @Mock private FailureRepository failureRepository;

    @InjectMocks private DeleteFailureUseCase useCase;

    @Test
    void execute_deletesFailure() {
        UUID id = UUID.randomUUID();
        Failure failure = Failure.builder().id(id).build();

        when(failureRepository.findById(id)).thenReturn(Optional.of(failure));

        useCase.execute(id);

        verify(failureRepository).delete(failure);
    }

    @Test
    void execute_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(failureRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(id));
        verify(failureRepository, never()).delete(any());
    }
}
