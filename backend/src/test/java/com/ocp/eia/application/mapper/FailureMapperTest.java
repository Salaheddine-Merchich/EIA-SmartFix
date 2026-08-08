package com.ocp.eia.application.mapper;

import com.ocp.eia.domain.model.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FailureMapperTest {

    private final FailureMapper mapper = Mappers.getMapper(FailureMapper.class);

    @Test
    void toResponse_withoutInterventions_hasNullLatestStatut() {
        Failure failure = Failure.builder()
                .id(UUID.randomUUID())
                .equipment(Equipment.builder().id(UUID.randomUUID()).code("EQ-1").designation("Test").build())
                .dateHeure(Instant.now())
                .criticite(Criticite.MOYENNE)
                .statut(StatutPanne.OUVERTE)
                .interventions(List.of())
                .build();

        var response = mapper.toResponse(failure);

        assertEquals(0, response.interventionCount());
        assertNull(response.latestInterventionStatut());
    }

    @Test
    void toResponse_withLatestBrouillonIntervention_mapsStatut() {
        Instant older = Instant.parse("2026-01-01T10:00:00Z");
        Instant newer = Instant.parse("2026-02-01T10:00:00Z");
        Failure failure = Failure.builder()
                .id(UUID.randomUUID())
                .equipment(Equipment.builder().id(UUID.randomUUID()).code("EQ-1").designation("Test").build())
                .dateHeure(Instant.now())
                .criticite(Criticite.MOYENNE)
                .statut(StatutPanne.OUVERTE)
                .interventions(List.of(
                        Intervention.builder()
                                .id(UUID.randomUUID())
                                .statutValidation(StatutValidation.VALIDEE)
                                .createdAt(older)
                                .build(),
                        Intervention.builder()
                                .id(UUID.randomUUID())
                                .statutValidation(StatutValidation.BROUILLON)
                                .createdAt(newer)
                                .build()
                ))
                .build();

        var response = mapper.toResponse(failure);

        assertEquals(2, response.interventionCount());
        assertEquals(StatutValidation.BROUILLON, response.latestInterventionStatut());
    }
}
