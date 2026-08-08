package com.ocp.eia.modules.maintenance.domain.service;

import com.ocp.eia.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InterventionWorkflowTest {

    @Test
    void submit_fromBrouillon_succeeds() {
        Intervention intervention = intervention(StatutValidation.BROUILLON);
        InterventionWorkflow.submit(intervention);
        assertEquals(StatutValidation.SOUMISE, intervention.getStatutValidation());
    }

    @Test
    void submit_fromValidee_throws() {
        Intervention intervention = intervention(StatutValidation.VALIDEE);
        assertThrows(Exception.class, () -> InterventionWorkflow.submit(intervention));
    }

    @Test
    void validate_approved_setsValidee() {
        Intervention intervention = intervention(StatutValidation.SOUMISE);
        User validateur = User.builder().id(UUID.randomUUID()).role(Role.RESPONSABLE_EIA).build();
        var result = InterventionWorkflow.validate(intervention, true, "OK", validateur);
        assertEquals(InterventionWorkflow.ValidationResult.APPROVED, result);
        assertEquals(StatutValidation.VALIDEE, intervention.getStatutValidation());
    }

    @Test
    void ensureEditable_validee_throws() {
        Intervention intervention = intervention(StatutValidation.VALIDEE);
        assertThrows(Exception.class, () ->
                InterventionWorkflow.ensureEditable(intervention, UUID.randomUUID(), Role.TECHNICIEN));
    }

    private Intervention intervention(StatutValidation statut) {
        User tech = User.builder().id(UUID.randomUUID()).role(Role.TECHNICIEN).build();
        return Intervention.builder()
                .statutValidation(statut)
                .technicien(tech)
                .failure(Failure.builder().id(UUID.randomUUID()).build())
                .build();
    }
}
