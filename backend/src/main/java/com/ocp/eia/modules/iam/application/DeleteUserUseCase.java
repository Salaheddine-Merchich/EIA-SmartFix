package com.ocp.eia.modules.iam.application;

import com.ocp.eia.domain.repository.UserRepository;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteUserUseCase {

    private final UserRepository userRepository;

    public void execute(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Utilisateur introuvable: " + id);
        }
        userRepository.deleteById(id);
    }
}
