package com.ocp.eia.modules.iam.application;

import com.ocp.eia.domain.repository.UserRepository;
import com.ocp.eia.presentation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteUserUseCaseTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private DeleteUserUseCase useCase;

    @Test
    void execute_deletesUser() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(true);

        useCase.execute(id);

        verify(userRepository).deleteById(id);
    }

    @Test
    void execute_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(id));
        verify(userRepository, never()).deleteById(any());
    }
}
