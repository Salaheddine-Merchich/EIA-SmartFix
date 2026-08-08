package com.ocp.eia.domain.repository;

import com.ocp.eia.domain.model.InterventionDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterventionDocumentRepository extends JpaRepository<InterventionDocument, UUID> {
    List<InterventionDocument> findByInterventionId(UUID interventionId);
    Optional<InterventionDocument> findByIdAndInterventionId(UUID id, UUID interventionId);
}
