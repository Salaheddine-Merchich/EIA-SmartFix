package com.ocp.eia.modules.analytics.application;

import com.ocp.eia.application.dto.AnalyticsDto.RecurringDefectItem;
import com.ocp.eia.application.dto.AnalyticsDto.RecurringDefectsAnalysisResponse;
import com.ocp.eia.modules.knowledge.domain.port.LlmProviderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyzeRecurringDefectsUseCase {

    private static final String DISCLAIMER =
            "Analyse indicative — les décisions de maintenance restent celles des responsables EIA.";

    private static final String SYSTEM_PROMPT = """
            Tu es un analyste maintenance EIA chez OCP.
            Tu identifies des schémas de défauts récurrents à partir de données agrégées.
            Tu ne poses JAMAIS de diagnostic définitif. Tu proposes des pistes d'analyse et de maintenance préventive.
            Réponds en français, en deux sections claires :
            ANALYSE: (paragraphe synthétique)
            RECOMMANDATIONS: (liste à puces concises)
            """;

    private final RecurringDefectsUseCase recurringDefectsUseCase;
    private final ObjectProvider<LlmProviderPort> llmProvider;

    public RecurringDefectsAnalysisResponse execute(int limit) {
        var aggregation = recurringDefectsUseCase.execute(limit);
        var defects = aggregation.defects();

        if (defects.isEmpty()) {
            return new RecurringDefectsAnalysisResponse(
                    defects,
                    "Aucun code défaut récurrent détecté (occurrence > 1).",
                    "Continuer la saisie systématique des codes défaut lors des déclarations de panne.",
                    DISCLAIMER
            );
        }

        String userPrompt = buildUserPrompt(defects);
        LlmProviderPort llm = llmProvider.getIfAvailable();
        if (llm == null) {
            return new RecurringDefectsAnalysisResponse(
                    defects,
                    buildFallbackAnalysis(defects),
                    buildFallbackRecommendations(defects),
                    DISCLAIMER
            );
        }

        try {
            String raw = llm.complete(SYSTEM_PROMPT, userPrompt);
            return new RecurringDefectsAnalysisResponse(
                    defects,
                    extractSection(raw, "ANALYSE"),
                    extractSection(raw, "RECOMMANDATIONS"),
                    DISCLAIMER
            );
        } catch (Exception e) {
            log.warn("Analyse IA défauts récurrents indisponible: {}", e.getMessage());
            return new RecurringDefectsAnalysisResponse(
                    defects,
                    buildFallbackAnalysis(defects),
                    buildFallbackRecommendations(defects),
                    DISCLAIMER
            );
        }
    }

    private String buildUserPrompt(java.util.List<RecurringDefectItem> defects) {
        String table = defects.stream()
                .map(d -> "- %s : %d occurrences, %d équipements, dernière vue %s".formatted(
                        d.codeDefaut(), d.occurrenceCount(), d.affectedEquipmentCount(), d.lastSeenMonth()))
                .collect(Collectors.joining("\n"));
        return "Voici les codes défaut récurrents agrégés :\n" + table;
    }

    private String buildFallbackAnalysis(java.util.List<RecurringDefectItem> defects) {
        RecurringDefectItem top = defects.getFirst();
        return "%d code(s) défaut se répètent. Le plus fréquent est « %s » avec %d occurrences sur %d équipement(s)."
                .formatted(defects.size(), top.codeDefaut(), top.occurrenceCount(), top.affectedEquipmentCount());
    }

    private String buildFallbackRecommendations(java.util.List<RecurringDefectItem> defects) {
        return defects.stream()
                .limit(3)
                .map(d -> "• Prioriser l'analyse root cause pour le code %s (%d occurrences)".formatted(
                        d.codeDefaut(), d.occurrenceCount()))
                .collect(Collectors.joining("\n"));
    }

    private String extractSection(String raw, String label) {
        int start = raw.indexOf(label + ":");
        if (start < 0) {
            return raw.trim();
        }
        start += label.length() + 1;
        int nextSection = raw.indexOf("\n", start);
        String rest = raw.substring(start).trim();
        if (label.equals("ANALYSE")) {
            int rec = rest.indexOf("RECOMMANDATIONS:");
            if (rec > 0) {
                return rest.substring(0, rec).trim();
            }
        }
        return rest;
    }
}
