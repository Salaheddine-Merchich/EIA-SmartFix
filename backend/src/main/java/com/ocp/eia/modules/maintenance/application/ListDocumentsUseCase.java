package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.InterventionDto.DocumentResponse;
import com.ocp.eia.application.mapper.InterventionMapper;
import com.ocp.eia.domain.repository.InterventionDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListDocumentsUseCase {

    private final InterventionDocumentRepository documentRepository;
    private final InterventionMapper interventionMapper;

    public List<DocumentResponse> execute(UUID interventionId) {
        return documentRepository.findByInterventionId(interventionId).stream()
                .map(interventionMapper::toDocumentResponse)
                .toList();
    }
}
