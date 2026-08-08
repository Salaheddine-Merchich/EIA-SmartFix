package com.ocp.eia.modules.maintenance.infrastructure.pdf;

import com.ocp.eia.domain.model.Criticite;
import com.ocp.eia.domain.model.StatutPanne;
import com.ocp.eia.domain.model.StatutValidation;

public final class PdfLabelMapper {

    private PdfLabelMapper() {
    }

    public static String criticite(Criticite criticite) {
        if (criticite == null) {
            return "—";
        }
        return switch (criticite) {
            case CRITIQUE -> "Critique";
            case HAUTE -> "Haute";
            case MOYENNE -> "Moyenne";
            case FAIBLE -> "Faible";
        };
    }

    public static String statutPanne(StatutPanne statut) {
        if (statut == null) {
            return "—";
        }
        return switch (statut) {
            case OUVERTE -> "Ouverte";
            case EN_COURS -> "En cours";
            case RESOLUE -> "Résolue";
            case CLOTUREE -> "Clôturée";
        };
    }

    public static String statutValidation(StatutValidation statut) {
        if (statut == null) {
            return "—";
        }
        return switch (statut) {
            case BROUILLON -> "Brouillon";
            case SOUMISE -> "Soumise";
            case VALIDEE -> "Validée";
            case REJETEE -> "Rejetée";
        };
    }

    public static String orDash(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }
}
