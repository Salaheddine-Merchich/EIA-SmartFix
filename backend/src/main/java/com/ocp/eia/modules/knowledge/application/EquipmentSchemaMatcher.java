package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.application.dto.AiDto.EquipmentSchemaDto;
import com.ocp.eia.domain.model.Equipment;
import com.ocp.eia.domain.model.EquipmentSchema;
import com.ocp.eia.domain.repository.EquipmentSchemaRepository;
import com.ocp.eia.modules.knowledge.domain.model.QuerySignals;
import com.ocp.eia.modules.knowledge.domain.model.SearchContext;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@RequiredArgsConstructor
public class EquipmentSchemaMatcher {

    private static final int MAX_EQUIPMENTS = 3;

    private final EquipmentSchemaRepository schemaRepository;

    public List<EquipmentSchemaDto> match(QuerySignals signals, SearchContext context) {
        if (signals == null) {
            return List.of();
        }

        List<EquipmentSchema> candidates = loadCandidates(signals, context);
        if (candidates.isEmpty()) {
            return List.of();
        }

        Set<String> queryTokens = buildQueryTokens(signals);
        UUID requestEquipmentId = context != null ? context.equipmentId() : null;

        Map<UUID, Long> schemaCountsByEquipment = loadSchemaCounts(candidates);

        List<ScoredSchema> ranked = candidates.stream()
                .map(schema -> new ScoredSchema(
                        schema,
                        score(schema, queryTokens, requestEquipmentId, signals, context)))
                .filter(scored -> scored.score > 0)
                .sorted(Comparator.comparingDouble(ScoredSchema::score).reversed())
                .toList();

        return selectOnePerEquipment(ranked).stream()
                .map(scored -> toDto(scored.schema, schemaCountsByEquipment))
                .toList();
    }

    private List<ScoredSchema> selectOnePerEquipment(List<ScoredSchema> ranked) {
        Map<UUID, ScoredSchema> bestByEquipment = new LinkedHashMap<>();
        for (ScoredSchema scored : ranked) {
            UUID equipmentId = scored.schema.getEquipment().getId();
            bestByEquipment.putIfAbsent(equipmentId, scored);
            if (bestByEquipment.size() >= MAX_EQUIPMENTS) {
                break;
            }
        }
        return new ArrayList<>(bestByEquipment.values());
    }

    private List<EquipmentSchema> loadCandidates(QuerySignals signals, SearchContext context) {
        String zone = firstNonBlank(
                context != null ? context.equipmentZone() : null,
                signals.equipmentZone().orElse(null)
        );
        String family = firstNonBlank(
                context != null ? context.equipmentFamily() : null,
                signals.equipmentFamily().orElse(null)
        );

        if (zone != null || family != null) {
            List<EquipmentSchema> scoped = schemaRepository.findActiveByZoneAndFamily(zone, family);
            if (!scoped.isEmpty()) {
                return scoped;
            }
        }

        return schemaRepository.findAllActiveWithEquipment();
    }

    private double score(
            EquipmentSchema schema,
            Set<String> queryTokens,
            UUID requestEquipmentId,
            QuerySignals signals,
            SearchContext context
    ) {
        double score = 0;
        Equipment equipment = schema.getEquipment();

        if (requestEquipmentId != null && equipment.getId().equals(requestEquipmentId)) {
            score += 5.0;
        }

        if (context != null && context.equipmentZone() != null && equipment.getZone() != null
                && equipment.getZone().toLowerCase(Locale.ROOT)
                .contains(context.equipmentZone().toLowerCase(Locale.ROOT))) {
            score += 2.0;
        }

        if (context != null && context.equipmentFamily() != null && equipment.getFamille() != null
                && equipment.getFamille().equalsIgnoreCase(context.equipmentFamily())) {
            score += 1.5;
        }

        score += equipmentRoleBoost(equipment, queryTokens);

        boolean keywordMatched = false;
        if (schema.getTriggerKeywords() != null) {
            for (String keyword : schema.getTriggerKeywords()) {
                if (keyword == null || keyword.isBlank()) {
                    continue;
                }
                String normalized = keyword.toLowerCase(Locale.ROOT);
                if (queryTokens.contains(normalized)) {
                    score += 3.0;
                    keywordMatched = true;
                    if (signals.faultCodes() != null
                            && signals.faultCodes().stream().anyMatch(code -> code.equalsIgnoreCase(normalized))) {
                        score += 3.0;
                    }
                } else if (containsToken(queryTokens, normalized)) {
                    score += 1.5;
                    keywordMatched = true;
                }
            }
        }

        double manufacturerBoost = manufacturerBoost(equipment, signals);
        score += manufacturerBoost;

        if (signals.manufacturer().isPresent()
                && manufacturerBoost == 0
                && (requestEquipmentId == null || !equipment.getId().equals(requestEquipmentId))) {
            if (signals.hasFaultCodes()) {
                if (!matchesFaultCodeKeyword(schema, signals)) {
                    return 0;
                }
            } else if (!keywordMatched) {
                return 0;
            }
        }

        if (schema.getCaption() != null) {
            String caption = schema.getCaption().toLowerCase(Locale.ROOT);
            for (String token : queryTokens) {
                if (token.length() >= 3 && caption.contains(token)) {
                    score += 0.5;
                }
            }
        }

        if (schema.getLabel() != null) {
            String label = schema.getLabel().toLowerCase(Locale.ROOT);
            for (String token : queryTokens) {
                if (token.length() >= 3 && label.contains(token)) {
                    score += 0.3;
                }
            }
        }

        if (signals.equipmentHint().isPresent()) {
            String hint = signals.equipmentHint().get();
            if (equipment.getDesignation() != null
                    && equipment.getDesignation().toLowerCase(Locale.ROOT).contains(hint)) {
                score += 1.0;
            }
        }

        return score;
    }

    private double equipmentRoleBoost(Equipment equipment, Set<String> queryTokens) {
        double boost = 0;
        String famille = equipment.getFamille() != null ? equipment.getFamille().toLowerCase(Locale.ROOT) : "";
        String code = equipment.getCode() != null ? equipment.getCode().toLowerCase(Locale.ROOT) : "";

        if (queryTokens.contains("pompe") && "pompe".equals(famille)) {
            boost += 2.5;
        }
        if (queryTokens.contains("moteur") && "moteur".equals(famille)) {
            boost += 2.0;
        }
        if ((queryTokens.contains("sonde") || queryTokens.contains("eau")) && code.contains("sen-eau")) {
            boost += 2.5;
        }
        if (queryTokens.contains("panneau") && code.contains("cap-pv")) {
            boost += 2.0;
        }
        return boost;
    }

    private double manufacturerBoost(Equipment equipment, QuerySignals signals) {
        if (signals.manufacturer().isEmpty()) {
            return 0;
        }
        String manufacturer = signals.manufacturer().get().toLowerCase(Locale.ROOT);
        String constructeur = equipment.getConstructeur() != null
                ? equipment.getConstructeur().toLowerCase(Locale.ROOT)
                : "";
        String code = equipment.getCode() != null ? equipment.getCode().toLowerCase(Locale.ROOT) : "";
        String designation = equipment.getDesignation() != null
                ? equipment.getDesignation().toLowerCase(Locale.ROOT)
                : "";

        if (!constructeur.isBlank() && constructeur.contains(manufacturer)) {
            return 2.0;
        }
        if ("goodrive".equals(manufacturer) && (code.contains("gd") || designation.contains("goodrive"))) {
            return 2.0;
        }
        if ("veichi".equals(manufacturer) && (constructeur.contains("veichi") || code.contains("vei"))) {
            return 2.0;
        }
        if ("hitachi".equals(manufacturer) && (constructeur.contains("hitachi") || code.contains("hit"))) {
            return 2.0;
        }
        if ("abb".equals(manufacturer) && (constructeur.contains("abb") || code.contains("abb") || code.contains("acs"))) {
            return 2.0;
        }
        return 0;
    }

    private Set<String> buildQueryTokens(QuerySignals signals) {
        Set<String> tokens = new HashSet<>();
        if (signals.faultCodes() != null) {
            signals.faultCodes().forEach(code -> tokens.add(code.toLowerCase(Locale.ROOT)));
        }
        if (signals.symptomKeywords() != null) {
            signals.symptomKeywords().forEach(k -> tokens.add(k.toLowerCase(Locale.ROOT)));
        }
        signals.equipmentHint().ifPresent(h -> tokens.add(h.toLowerCase(Locale.ROOT)));
        signals.equipmentFamily().ifPresent(f -> tokens.add(f.toLowerCase(Locale.ROOT)));
        signals.equipmentZone().ifPresent(z -> {
            tokens.add(z.toLowerCase(Locale.ROOT));
            if (z.toLowerCase(Locale.ROOT).contains("pv")) {
                tokens.add("pv");
                tokens.add("pompe");
            }
        });
        signals.manufacturer().ifPresent(m -> tokens.add(m.toLowerCase(Locale.ROOT)));
        return tokens;
    }

    private boolean containsToken(Set<String> queryTokens, String keyword) {
        for (String token : queryTokens) {
            if (token.contains(keyword) || keyword.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesFaultCodeKeyword(EquipmentSchema schema, QuerySignals signals) {
        if (schema.getTriggerKeywords() == null || !signals.hasFaultCodes()) {
            return false;
        }
        for (String code : signals.faultCodes()) {
            for (String keyword : schema.getTriggerKeywords()) {
                if (keyword != null && keyword.equalsIgnoreCase(code)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Map<UUID, Long> loadSchemaCounts(List<EquipmentSchema> candidates) {
        Map<UUID, Long> counts = new HashMap<>();
        for (EquipmentSchema schema : candidates) {
            UUID equipmentId = schema.getEquipment().getId();
            counts.computeIfAbsent(equipmentId, id -> schemaRepository.countByEquipmentIdAndActiveTrue(id));
        }
        return counts;
    }

    private EquipmentSchemaDto toDto(EquipmentSchema schema, Map<UUID, Long> schemaCountsByEquipment) {
        UUID equipmentId = schema.getEquipment().getId();
        UUID schemaId = schema.getId();
        long totalSchemas = schemaCountsByEquipment.getOrDefault(equipmentId, 1L);
        return new EquipmentSchemaDto(
                schemaId,
                equipmentId,
                schema.getEquipment().getCode(),
                schema.getEquipment().getDesignation(),
                schema.getLabel(),
                schema.getSchemaType(),
                schema.getSourcePdf(),
                schema.getSourcePage(),
                schema.getCaption(),
                (int) totalSchemas,
                "/api/v1/equipment/" + equipmentId + "/schemas/" + schemaId + "/download"
        );
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private record ScoredSchema(EquipmentSchema schema, double score) {}
}
