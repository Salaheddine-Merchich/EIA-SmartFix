package com.ocp.eia.modules.iam.application;

import com.ocp.eia.application.dto.UserDto.UserResponse;
import com.ocp.eia.application.mapper.UserMapper;
import com.ocp.eia.domain.model.Role;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListUsersUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks private ListUsersUseCase useCase;

    @Test
    void execute_returnsMappedList() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("admin@ocp.ma")
                .role(Role.ADMIN)
                .build();
        UserResponse response = mock(UserResponse.class);
        List<UserResponse> mapped = List.of(response);

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userMapper.toResponseList(List.of(user))).thenReturn(mapped);

        List<UserResponse> result = useCase.execute();

        assertSame(mapped, result);
        assertEquals(1, result.size());
    }

    @Test
    void execute_emptyList_returnsEmpty() {
        when(userRepository.findAll()).thenReturn(List.of());
        when(userMapper.toResponseList(List.of())).thenReturn(List.of());

        assertEquals(0, useCase.execute().size());
    }
}
