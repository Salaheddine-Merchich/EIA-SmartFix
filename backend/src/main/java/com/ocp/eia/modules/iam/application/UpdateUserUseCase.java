package com.ocp.eia.modules.iam.application;

import com.ocp.eia.application.dto.UserDto.UserResponse;
import com.ocp.eia.application.dto.UserDto.UserUpdateRequest;
import com.ocp.eia.application.mapper.UserMapper;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.domain.repository.UserRepository;
import com.ocp.eia.shared.exception.ConflictException;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateUserUseCase {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserResponse execute(UUID id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable: " + id));
        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Un utilisateur avec cet email existe déjà");
        }
        user.setEmail(request.email());
        user.setRole(request.role());
        user.setNomPrenom(request.nomPrenom());
        user.setActif(request.actif());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return userMapper.toResponse(userRepository.save(user));
    }
}
