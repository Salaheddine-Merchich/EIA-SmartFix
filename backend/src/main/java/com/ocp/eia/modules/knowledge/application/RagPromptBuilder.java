package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import com.ocp.eia.modules.knowledge.domain.model.SimilarKnowledgeDocument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
public class RagPromptBuilder {

    private static final String SYSTEM_PROMPT = """
            Tu es un assistant technique EIA chez OCP (Office Chérifien des Phosphates).
            Tu ne poses JAMAIS de diagnostic définitif. Tu analyses les interventions passées similaires et proposes des pistes.
            Le technicien prend toujours la décision finale.
            
            INSTRUCTIONS CRITIQUES pour les correctiveActions:
            - Tu dois TOUJOURS générer entre 2 et 5 actions correctives précises et techniques
            - Chaque action doit être spécifique, avec des références techniques quand possible
            - Inclure les codes de pièces, références constructeurs, procédures normalisées
            - Ordonner par priorité d'intervention (urgence puis préventif)
            - Utiliser un vocabulaire technique professionnel
            
            EXEMPLES d'actions correctives de qualité:
            - "Vérifier l'isolement électrique avec mégohmmètre (spec >5 MΩ à 500V)"
            - "Remplacer roulement SKF 6312 côté accouplement selon procédure MT-R-001"
            - "Effectuer étalonnage 2 points avec configurateur HART selon norme ISA-5.1"
            - "Contrôler tension bobine contacteur (nominal 220V ±10%)"
            - "Appliquer couple de serrage 45 N.m sur brides DN150 selon DIN 2633"
            
            Réponds UNIQUEMENT en JSON valide avec cette structure exacte:
            {
                "probableCauses": ["cause technique 1", "cause technique 2"],
                "correctiveActions": [
                    "Action prioritaire avec référence technique",
                    "Action complémentaire avec procédure",
                    "Action préventive avec norme/standard"
                ],
                "summary": "Diagnostic technique basé sur les interventions similaires",
                "advice": "Conseil de maintenance préventive avec références"
            }
            
            IMPORTANT: Si un code défaut est mentionné dans la requête, base ton analyse UNIQUEMENT sur les interventions dont le code correspond. Si aucune intervention ne correspond à ce code, indique-le explicitement dans le summary.
            Ne JAMAIS laisser correctiveActions vide. En l'absence d'intervention pertinente, limite-toi à des vérifications documentées sans inventer de diagnostic.
            """;

    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String userPrompt(
            String description,
            List<SimilarIntervention> relevant,
            List<SimilarKnowledgeDocument> knowledgeDocs
    ) {
        StringBuilder userPromptBuilder = new StringBuilder();
        userPromptBuilder.append("Description de la panne actuelle:\n").append(description).append("\n\n");

        if (!relevant.isEmpty()) {
            userPromptBuilder.append("Interventions passées similaires (validées):\n")
                    .append(buildInterventionContext(relevant)).append("\n\n");
        }

        if (!knowledgeDocs.isEmpty()) {
            userPromptBuilder.append("Documentation technique pertinente:\n")
                    .append(buildKnowledgeContext(knowledgeDocs)).append("\n\n");
        }

        userPromptBuilder.append("Analyse la situation et propose:\n")
                .append("- CAUSES PROBABLES: Identifie 2-4 causes techniques possibles basées sur les symptômes\n")
                .append("- ACTIONS CORRECTIVES: Fournis 2-5 actions techniques précises avec références (pièces, procédures, normes)\n")
                .append("- RÉSUMÉ: Synthèse technique du diagnostic\n")
                .append("- CONSEILS: Recommandations de maintenance préventive\n\n")
                .append("Concentre-toi sur des actions correctives concrètes et réalisables par un technicien qualifié.");

        return userPromptBuilder.toString();
    }

    private String buildInterventionContext(List<SimilarIntervention> similar) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < similar.size(); i++) {
            SimilarIntervention row = similar.get(i);
            sb.append("--- Intervention ").append(i + 1).append(" (équipement: ").append(row.equipmentCode());
            if (row.faultCode() != null && !row.faultCode().isBlank()) {
                sb.append(", code: ").append(row.faultCode());
            }
            sb.append(") ---\n");
            if (row.symptomes() != null) {
                sb.append("Symptômes: ").append(truncateField(row.symptomes())).append("\n");
            }
            if (row.actionsCorrectives() != null) {
                sb.append("Actions: ").append(truncateField(row.actionsCorrectives())).append("\n");
            }
        }
        return sb.toString();
    }

    private String buildKnowledgeContext(List<SimilarKnowledgeDocument> knowledgeDocuments) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < knowledgeDocuments.size(); i++) {
            SimilarKnowledgeDocument doc = knowledgeDocuments.get(i);
            sb.append("--- Document ").append(i + 1).append(" (").append(doc.documentType()).append(") ---\n");
            if (doc.equipmentFamily() != null) {
                sb.append("Famille: ").append(doc.equipmentFamily()).append("\n");
            }
            sb.append("Extrait: ").append(truncateField(doc.contentExcerpt())).append("\n");
        }
        return sb.toString();
    }

    private String truncateField(String field) {
        if (field == null || field.length() <= 120) {
            return field;
        }
        return field.substring(0, 117) + "...";
    }
}
