package com.ocp.eia.modules.maintenance.domain.service;

import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.model.Role;
import com.ocp.eia.domain.model.StatutValidation;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.shared.exception.DomainRuleViolationException;

import java.time.Instant;
import java.util.UUID;

/**
 * Règles métier pures du workflow intervention — indépendant de Spring et JPA.
 */
public final class InterventionWorkflow {

    private InterventionWorkflow() {}

    public static void ensureEditable(Intervention intervention, UUID currentUserId, Role currentRole) {
        if (intervention.getStatutValidation() == StatutValidation.VALIDEE) {
            throw new DomainRuleViolationException(
                    DomainRuleViolationException.ViolationType.FORBIDDEN,
                    "Une intervention validée ne peut plus être modifiée"
            );
        }
        if (currentRole == Role.TECHNICIEN
                && !intervention.getTechnicien().getId().equals(currentUserId)) {
            throw new DomainRuleViolationException(
                    DomainRuleViolationException.ViolationType.FORBIDDEN,
                    "Vous ne pouvez modifier que vos propres interventions"
            );
        }
    }

    public static void submit(Intervention intervention) {
        StatutValidation statut = intervention.getStatutValidation();
        if (statut != StatutValidation.BROUILLON && statut != StatutValidation.REJETEE) {
            throw new DomainRuleViolationException(
                    DomainRuleViolationException.ViolationType.BAD_REQUEST,
                    "Seules les interventions en brouillon ou rejetées peuvent être soumises"
            );
        }
        intervention.setStatutValidation(StatutValidation.SOUMISE);
    }

    public static ValidationResult validate(Intervention intervention, boolean approved, String commentaire, User validateur) {
        if (intervention.getStatutValidation() != StatutValidation.SOUMISE) {
            throw new DomainRuleViolationException(
                    DomainRuleViolationException.ViolationType.BAD_REQUEST,
                    "Seules les interventions soumises peuvent être validées"
            );
        }
        intervention.setValidateur(validateur);
        intervention.setDateValidation(Instant.now());
        intervention.setCommentaireValidation(commentaire);

        if (approved) {
            intervention.setStatutValidation(StatutValidation.VALIDEE);
            return ValidationResult.APPROVED;
        }
        intervention.setStatutValidation(StatutValidation.REJETEE);
        return ValidationResult.REJECTED;
    }

    public enum ValidationResult {
        APPROVED,
        REJECTED
    }
}
