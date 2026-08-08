package com.ocp.eia.modules.iam.application;

import com.ocp.eia.application.dto.UserDto.UserResponse;
import com.ocp.eia.application.mapper.UserMapper;
import com.ocp.eia.domain.model.Role;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.domain.repository.UserRepository;
import com.ocp.eia.presentation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindUserByIdUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks private FindUserByIdUseCase useCase;

    @Test
    void execute_returnsMappedResponse() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).email("admin@ocp.ma").role(Role.ADMIN).build();
        UserResponse response = mock(UserResponse.class);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        assertSame(response, useCase.execute(id));
    }

    @Test
    void execute_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(id));
    }
}
