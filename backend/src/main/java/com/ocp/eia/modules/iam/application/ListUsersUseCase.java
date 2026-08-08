package com.ocp.eia.modules.iam.application;

import com.ocp.eia.domain.model.Role;
import com.ocp.eia.application.dto.UserDto.UserResponse;
import com.ocp.eia.application.mapper.UserMapper;
import com.ocp.eia.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListUsersUseCase {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public List<UserResponse> execute() {
        return userMapper.toResponseList(userRepository.findAll());
    }

    public List<UserResponse> executeAssignable() {
        return userMapper.toResponseList(
                userRepository.findAll().stream()
                        .filter(u -> u.isActif() && (u.getRole() == Role.RESPONSABLE_EIA || u.getRole() == Role.ADMIN))
                        .toList()
        );
    }
}
