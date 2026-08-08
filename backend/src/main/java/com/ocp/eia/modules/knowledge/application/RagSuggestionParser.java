package com.ocp.eia.modules.knowledge.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocp.eia.application.dto.AiDto.AiSuggestions;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import com.ocp.eia.modules.knowledge.domain.model.SimilarKnowledgeDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RagSuggestionParser {

    private final ObjectMapper objectMapper;

    public AiSuggestions parse(String llmResponse) throws JsonProcessingException {
        String json = extractJson(llmResponse);
        var node = objectMapper.readTree(json);

        List<String> probableCauses = toStringList(node.get("probableCauses"));
        List<String> correctiveActions = toStringList(node.get("correctiveActions"));
        String summary = node.path("summary").asText("");
        String advice = node.path("advice").asText("");

        if (correctiveActions.isEmpty()) {
            log.warn("LLM a généré des correctiveActions vides, application du fallback technique");
            correctiveActions = List.of(
                    "Effectuer diagnostic approfondi avec documentation technique",
                    "Vérifier paramètres de fonctionnement et conformité aux spécifications",
                    "Consulter responsable EIA pour intervention spécialisée"
            );
        }

        if (probableCauses.isEmpty()) {
            log.warn("LLM a généré des probableCauses vides, application du fallback");
            probableCauses = List.of("Analyse technique requise pour diagnostic précis");
        }

        if (summary.isBlank()) {
            summary = "Diagnostic technique basé sur analyse des données disponibles";
        }

        if (advice.isBlank()) {
            advice = "Valider diagnostic avant intervention et documenter l'action corrective";
        }

        return new AiSuggestions(probableCauses, correctiveActions, summary, advice);
    }

    public AiSuggestions fallbackFromHistory(
            List<SimilarIntervention> similar,
            List<SimilarKnowledgeDocument> knowledgeDocuments
    ) {
        List<String> causes = new ArrayList<>();
        List<String> actions = new ArrayList<>();

        similar.stream()
                .map(SimilarIntervention::causeRacine)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .limit(3)
                .forEach(cause -> {
                    String[] parts = cause.split("[.;]");
                    for (String part : parts) {
                        String cleaned = part.trim();
                        if (cleaned.length() > 15) {
                            causes.add(cleaned);
                            if (causes.size() >= 3) {
                                break;
                            }
                        }
                    }
                });

        similar.stream()
                .map(SimilarIntervention::actionsCorrectives)
                .filter(a -> a != null && !a.isBlank())
                .distinct()
                .limit(4)
                .forEach(actionText -> {
                    String[] parts = actionText.split("[.;,]");
                    for (String part : parts) {
                        String cleaned = part.trim();
                        if (cleaned.length() > 20 && !cleaned.toLowerCase().startsWith("remplacer")
                                && actions.stream().noneMatch(existing ->
                                existing.contains(cleaned.substring(0, Math.min(cleaned.length(), 30))))) {
                            String finalAction = cleaned;
                            if (cleaned.toLowerCase().contains("vérifier") && !cleaned.contains("avec")) {
                                finalAction = cleaned + " avec instruments de mesure appropriés";
                            }
                            actions.add(finalAction);
                            if (actions.size() >= 4) {
                                break;
                            }
                        }
                    }
                });

        if (!knowledgeDocuments.isEmpty()) {
            boolean hasManual = knowledgeDocuments.stream().anyMatch(d -> "manual".equals(d.documentType()));
            boolean hasProcedure = knowledgeDocuments.stream().anyMatch(d -> "procedure".equals(d.documentType()));

            if (hasManual && causes.size() < 3) {
                causes.add("Consulter manuel constructeur pour diagnostic détaillé");
            }
            if (hasProcedure && actions.size() < 3) {
                actions.add("Suivre procédure maintenance préventive selon documentation technique");
            }
        }

        if (actions.isEmpty()) {
            actions.addAll(List.of(
                    "Effectuer inspection visuelle complète de l'équipement",
                    "Vérifier alimentations électriques et connexions",
                    "Contrôler paramètres de fonctionnement nominal"
            ));
        } else if (actions.size() == 1) {
            actions.add("Documenter l'intervention pour enrichir la base de connaissances");
        }

        if (causes.isEmpty()) {
            causes.addAll(List.of(
                    "Défaillance composant ou usure normale",
                    "Paramètres de fonctionnement hors tolérances"
            ));
        }

        String summary = similar.isEmpty() && knowledgeDocuments.isEmpty()
                ? "Diagnostic basé sur procédures générales de maintenance industrielle"
                : String.format("Diagnostic basé sur %d intervention(s) similaire(s) et %d document(s) technique(s)",
                similar.size(), knowledgeDocuments.size());

        String advice = similar.isEmpty() && knowledgeDocuments.isEmpty()
                ? "Consulter un expert technique ou le responsable EIA pour cas complexes non documentés"
                : "Valider diagnostic avec interventions similaires avant action corrective";

        return new AiSuggestions(causes, actions, summary, advice);
    }

    public AiSuggestions noEvidenceFallback() {
        return new AiSuggestions(
                List.of("Aucune intervention similaire validée trouvée"),
                List.of("Consulter la documentation constructeur"),
                "Pas assez de données historiques validées pour cette description.",
                "Documentez cette intervention pour enrichir la base de connaissances."
        );
    }

    private List<String> toStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> list.add(n.asText()));
        }
        return list;
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }
}
