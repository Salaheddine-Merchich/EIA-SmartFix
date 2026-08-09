package com.ocp.eia.modules.iam.application;

import com.ocp.eia.application.dto.UserDto.UserRequest;
import com.ocp.eia.application.dto.UserDto.UserResponse;
import com.ocp.eia.application.mapper.UserMapper;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.domain.repository.UserRepository;
import com.ocp.eia.shared.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateUserUseCase {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserResponse execute(UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Un utilisateur avec cet email existe déjà");
        }
        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        return userMapper.toResponse(userRepository.save(user));
    }
}
