package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.domain.model.Failure;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteFailureUseCase {

    private final FailureRepository failureRepository;

    public void execute(UUID id) {
        Failure failure = failureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Panne introuvable: " + id));
        failureRepository.delete(failure);
    }
}
