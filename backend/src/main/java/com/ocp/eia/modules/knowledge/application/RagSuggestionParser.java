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
import java.util.Locale;

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
        if (similar == null || similar.isEmpty()) {
            return insufficientEvidenceFallback();
        }

        List<String> causes = new ArrayList<>();
        List<String> actions = new ArrayList<>();

        similar.stream()
                .map(SimilarIntervention::causeRacine)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .limit(3)
                .forEach(cause -> {
                    String[] parts = splitActionSegments(cause);
                    for (String part : parts) {
                        String cleaned = part.trim();
                        if (cleaned.length() > 10) {
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
                    String[] parts = splitActionSegments(actionText);
                    for (String part : parts) {
                        String cleaned = part.trim();
                        if (isActionCandidate(cleaned) && actions.stream().noneMatch(existing ->
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

        int documentCount = knowledgeDocuments == null ? 0 : knowledgeDocuments.size();
        if (causes.isEmpty() || actions.isEmpty()) {
            return insufficientEvidenceFallback();
        }

        String summary = String.format(
                "Diagnostic basé sur %d intervention(s) similaire(s) et %d document(s) technique(s)",
                similar.size(),
                documentCount
        );
        String advice = "Valider diagnostic avec interventions similaires avant action corrective";

        return new AiSuggestions(causes, actions, summary, advice);
    }

    public AiSuggestions noEvidenceFallback() {
        return insufficientEvidenceFallback();
    }

    public AiSuggestions vagueQueryFallback() {
        return new AiSuggestions(
                List.of("Description trop vague pour établir un diagnostic"),
                List.of("Précisez l'équipement, le symptôme ou un code défaut (ex. E21)"),
                "Impossible d'analyser cette description.",
                "Reformulez avec plus de détails avant de relancer l'assistant."
        );
    }

    public AiSuggestions insufficientEvidenceFallback() {
        return new AiSuggestions(
                List.of("Aucune intervention validée ni document technique suffisamment proche"),
                List.of(
                        "Précisez l'équipement, la zone ou un code défaut connu dans le parc",
                        "Consultez une fiche panne existante ou le manuel constructeur en direct"
                ),
                "Cette description ne correspond à aucune donnée fiable du projet.",
                "Reformulez avec un symptôme concret (ex. « Pompe PV ne démarre plus ») ou un code défaut (ex. E21)."
        );
    }

    public AiSuggestions unknownFaultCodeFallback(String faultCode) {
        String code = faultCode != null ? faultCode : "inconnu";
        return new AiSuggestions(
                List.of("Le code défaut " + code + " n'existe pas dans la base de connaissances validée"),
                List.of(
                        "Vérifier le code affiché sur l'équipement ou le variateur",
                        "Consulter le manuel constructeur pour le libellé exact du défaut"
                ),
                "Aucune intervention validée ne correspond au code " + code + ".",
                "Documentez l'intervention après résolution pour enrichir la base."
        );
    }

    /**
     * Segmente sur ; et , sans couper les références paramètres (ex. F14.11).
     */
    static String[] splitActionSegments(String text) {
        if (text == null || text.isBlank()) {
            return new String[0];
        }
        return text.split("\\s*[;,]\\s*");
    }

    private static boolean isActionCandidate(String cleaned) {
        if (cleaned == null || cleaned.isBlank()) {
            return false;
        }
        String lower = cleaned.toLowerCase(Locale.ROOT);
        if (lower.startsWith("augmenter") || lower.startsWith("ajuster") || lower.startsWith("vérifier")
                || lower.startsWith("verifier")) {
            return cleaned.length() >= 8;
        }
        return cleaned.length() >= 12 && !lower.startsWith("remplacer");
    }

    private List<String> toStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> list.add(n.asText()));
        }
        return list;
    }

    private String extractJson(String response) {
        if (response == null || response.isBlank()) {
            return "{}";
        }

        String trimmed = response.trim();

        // Strip markdown code fences: ```json ... ``` or ``` ... ```
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            int fenceEnd = trimmed.lastIndexOf("```");
            if (fenceEnd >= 0) {
                trimmed = trimmed.substring(0, fenceEnd).trim();
            }
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}
