package com.ocp.eia.modules.iam.application;

import com.ocp.eia.application.dto.UserDto.UserRequest;
import com.ocp.eia.application.dto.UserDto.UserResponse;
import com.ocp.eia.application.mapper.UserMapper;
import com.ocp.eia.domain.model.Role;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.domain.repository.UserRepository;
import com.ocp.eia.presentation.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private CreateUserUseCase useCase;

    @Test
    void execute_createsUserWithEncodedPassword() {
        UserRequest request = new UserRequest("new@ocp.ma", "Password123!", Role.TECHNICIEN, "New User", true);
        User entity = User.builder().email("new@ocp.ma").role(Role.TECHNICIEN).build();
        UserResponse response = mock(UserResponse.class);

        when(userRepository.existsByEmail("new@ocp.ma")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(entity);
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded-hash");
        when(userRepository.save(entity)).thenReturn(entity);
        when(userMapper.toResponse(entity)).thenReturn(response);

        assertSame(response, useCase.execute(request));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("encoded-hash", captor.getValue().getPasswordHash());
    }

    @Test
    void execute_duplicateEmail_throws() {
        UserRequest request = new UserRequest("exists@ocp.ma", "Password123!", Role.ADMIN, "Dup", true);
        when(userRepository.existsByEmail("exists@ocp.ma")).thenReturn(true);

        assertThrows(ConflictException.class, () -> useCase.execute(request));
        verify(userRepository, never()).save(any());
    }
}
