package com.ocp.eia.modules.iam.application;

import com.ocp.eia.application.dto.UserDto.UserResponse;
import com.ocp.eia.application.dto.UserDto.UserUpdateRequest;
import com.ocp.eia.application.mapper.UserMapper;
import com.ocp.eia.domain.model.Role;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.domain.repository.UserRepository;
import com.ocp.eia.shared.exception.ConflictException;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateUserUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks private UpdateUserUseCase useCase;

    @Test
    void execute_updatesUser() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).email("old@ocp.ma").role(Role.TECHNICIEN).nomPrenom("Old").actif(true).build();
        UserUpdateRequest request = new UserUpdateRequest("old@ocp.ma", Role.RESPONSABLE_EIA, "Updated", false, null);
        UserResponse response = mock(UserResponse.class);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        assertSame(response, useCase.execute(id, request));
        assertEquals(Role.RESPONSABLE_EIA, user.getRole());
        assertEquals("Updated", user.getNomPrenom());
        assertFalse(user.isActif());
        verify(passwordEncoder, never()).encode(any());
        verify(refreshTokenService).revokeAllForUser(id);
    }

    @Test
    void execute_passwordChange_revokesRefreshTokens() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).email("user@ocp.ma").role(Role.TECHNICIEN).nomPrenom("Tech").actif(true).build();
        UserUpdateRequest request = new UserUpdateRequest("user@ocp.ma", Role.TECHNICIEN, "Tech", true, "NewPass123!");
        UserResponse response = mock(UserResponse.class);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPass123!")).thenReturn("new-hash");
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        assertSame(response, useCase.execute(id, request));
        verify(refreshTokenService).revokeAllForUser(id);
    }

    @Test
    void execute_roleChangeOnly_doesNotRevokeRefreshTokens() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).email("user@ocp.ma").role(Role.TECHNICIEN).nomPrenom("Tech").actif(true).build();
        UserUpdateRequest request = new UserUpdateRequest("user@ocp.ma", Role.RESPONSABLE_EIA, "Tech", true, null);
        UserResponse response = mock(UserResponse.class);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        assertSame(response, useCase.execute(id, request));
        verify(refreshTokenService, never()).revokeAllForUser(any());
    }

    @Test
    void execute_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        UserUpdateRequest request = new UserUpdateRequest("a@ocp.ma", Role.ADMIN, "X", true, null);
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(id, request));
    }

    @Test
    void execute_emailConflict_throws() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).email("old@ocp.ma").build();
        UserUpdateRequest request = new UserUpdateRequest("taken@ocp.ma", Role.ADMIN, "X", true, null);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@ocp.ma")).thenReturn(true);

        assertThrows(ConflictException.class, () -> useCase.execute(id, request));
        verify(userRepository, never()).save(any());
    }
}
