package com.ocp.eia.modules.knowledge.infrastructure.persistence;



import com.ocp.eia.modules.knowledge.application.SymptomQueryExpander;

import com.ocp.eia.modules.knowledge.domain.model.QuerySignals;

import com.ocp.eia.modules.knowledge.domain.model.SearchContext;

import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;

import com.ocp.eia.modules.knowledge.domain.port.InterventionTextSearchPort;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.stereotype.Component;



import java.util.ArrayList;

import java.util.LinkedHashSet;

import java.util.List;

import java.util.Locale;

import java.util.Set;

import java.util.UUID;



@Component

@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")

@RequiredArgsConstructor

public class PgInterventionTextSearchAdapter implements InterventionTextSearchPort {



    private static final int BOOST_OVERSAMPLE_FACTOR = 5;

    private static final double CODE_MATCH_SCORE = 0.95;

    private static final double SYMPTOM_MATCH_SCORE = 0.88;



    private static final Set<String> STOPWORDS = Set.of(

            "le", "la", "les", "de", "du", "des", "un", "une", "et", "ou", "sur", "avec", "pour",

            "dans", "par", "en", "au", "aux", "ce", "cette", "est", "son", "sa", "ses", "the", "plus"

    );



    private final JdbcTemplate jdbcTemplate;



    @Override

    public List<SimilarIntervention> searchValidated(String query, int topK) {

        return searchValidated(query, topK, SearchContext.none());

    }



    @Override

    public List<SimilarIntervention> searchValidated(String query, int topK, SearchContext context) {

        return searchValidated(query, topK, context, QuerySignals.empty());

    }



    @Override

    public List<SimilarIntervention> searchValidated(String query, int topK, SearchContext context, QuerySignals signals) {

        if (query == null || query.isBlank()) {

            return List.of();

        }



        String ftsQuery = SymptomQueryExpander.buildFtsQuery(query, signals);

        List<String> likePatterns = buildLikePatterns(query, signals);



        StringBuilder queryBuilder = new StringBuilder("""

                SELECT i.id AS intervention_id,

                       i.symptomes,

                       i.cause_racine,

                       i.actions_correctives,

                       i.analyse_technique,

                       e.code AS equipment_code,

                       e.id AS equipment_id,

                       e.famille AS equipment_family,

                       e.zone AS equipment_zone,

                       f.code_defaut AS fault_code,

                       f.description_initiale AS failure_description,

                       e.constructeur AS constructeur,

                       GREATEST(

                           COALESCE(ts_rank(

                               i.search_vector,

                               plainto_tsquery('french', ?)

                           ), 0),

                           CASE WHEN %s THEN %f ELSE 0 END,

                           CASE WHEN %s THEN %f ELSE 0 END

                       ) AS base_similarity

                FROM interventions i

                JOIN failures f ON f.id = i.failure_id

                JOIN equipment e ON e.id = f.equipment_id

                WHERE i.statut_validation = 'VALIDEE'

                AND i.search_vector IS NOT NULL

                AND (

                    i.search_vector @@ plainto_tsquery('french', ?)

                    OR %s

                )

                """.formatted(

                buildCodeMatchCase(likePatterns), CODE_MATCH_SCORE,

                buildSymptomMatchCase(likePatterns), SYMPTOM_MATCH_SCORE,

                buildOrLikeClause(likePatterns)

        ));



        List<Object> params = new ArrayList<>();

        params.add(ftsQuery);
        appendLikeParams(params, likePatterns);
        appendSymptomLikeParams(params, likePatterns);
        params.add(ftsQuery);
        appendFullLikeParams(params, likePatterns);



        int fetchLimit = context.hasFilters()

                ? Math.max(topK, topK * BOOST_OVERSAMPLE_FACTOR)

                : topK;



        queryBuilder.append(" ORDER BY base_similarity DESC LIMIT ?");

        params.add(fetchLimit);



        List<SimilarIntervention> results = jdbcTemplate.query(queryBuilder.toString(),

                (rs, rowNum) -> mapRow(rs, context, signals),

                params.toArray());



        if (context.hasFilters()) {

            results.sort((a, b) -> Double.compare(b.similarity(), a.similarity()));

            if (results.size() > topK) {

                return results.subList(0, topK);

            }

        }



        return results;

    }



    private SimilarIntervention mapRow(

            java.sql.ResultSet rs,

            SearchContext context,

            QuerySignals signals

    ) throws java.sql.SQLException {

        UUID equipmentId = UUID.fromString(rs.getString("equipment_id"));

        String family = rs.getString("equipment_family");

        String zone = rs.getString("equipment_zone");

        String constructeur = rs.getString("constructeur");

        double baseSimilarity = rs.getDouble("base_similarity");



        double boost = context.calculateBoost(equipmentId, family, zone, constructeur);

        double symptomBoost = computeSymptomBoost(rs, signals);

        double finalSimilarity = Math.min(1.0, baseSimilarity * boost * symptomBoost);



        return new SimilarIntervention(

                UUID.fromString(rs.getString("intervention_id")),

                rs.getString("equipment_code"),

                rs.getString("symptomes"),

                rs.getString("cause_racine"),

                rs.getString("actions_correctives"),

                rs.getString("analyse_technique"),

                finalSimilarity,

                rs.getString("fault_code"),

                constructeur,

                family,

                zone

        );

    }



    private static double computeSymptomBoost(java.sql.ResultSet rs, QuerySignals signals) throws java.sql.SQLException {

        if (signals.symptomKeywords() == null || signals.symptomKeywords().isEmpty()) {

            return 1.0;

        }

        String combined = String.join(" ",

                rs.getString("symptomes"),

                rs.getString("cause_racine"),

                rs.getString("failure_description")

        );

        int overlap = SymptomQueryExpander.countSymptomOverlap(combined, signals.symptomKeywords());

        if (overlap >= 2) {

            return 1.4;

        }

        if (overlap == 1) {

            return 1.15;

        }

        return 1.0;

    }



    private static List<String> buildLikePatterns(String query, QuerySignals signals) {

        Set<String> patterns = new LinkedHashSet<>();



        if (signals.hasFaultCodes()) {

            for (String code : signals.faultCodes()) {

                patterns.add("%" + code.trim() + "%");

            }

        }



        if (signals.symptomKeywords() != null) {

            for (String keyword : signals.symptomKeywords()) {

                if (keyword.length() > 2) {

                    patterns.add("%" + keyword + "%");

                }

            }

        }



        for (String token : query.trim().split("\\s+")) {

            String normalized = token.toLowerCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9\\.\\-]", "");

            if (normalized.length() > 2 && !STOPWORDS.contains(normalized)) {

                patterns.add("%" + normalized + "%");

            }

        }



        if (patterns.isEmpty()) {

            patterns.add("%" + query.trim() + "%");

        }



        return List.copyOf(patterns);

    }



    private static String buildCodeMatchCase(List<String> likePatterns) {

        List<String> clauses = new ArrayList<>();

        for (int i = 0; i < likePatterns.size(); i++) {

            clauses.add("f.code_defaut ILIKE ? OR e.code ILIKE ? OR e.constructeur ILIKE ?");

        }

        return String.join(" OR ", clauses);

    }



    private static String buildSymptomMatchCase(List<String> likePatterns) {

        List<String> clauses = new ArrayList<>();

        for (int i = 0; i < likePatterns.size(); i++) {

            clauses.add(

                    "i.symptomes ILIKE ? OR i.cause_racine ILIKE ? OR f.description_initiale ILIKE ?"

            );

        }

        return String.join(" OR ", clauses);

    }



    private static String buildOrLikeClause(List<String> likePatterns) {

        List<String> clauses = new ArrayList<>();

        for (int i = 0; i < likePatterns.size(); i++) {

            clauses.add(

                    "f.code_defaut ILIKE ? OR e.code ILIKE ? OR e.constructeur ILIKE ?"

                            + " OR i.symptomes ILIKE ? OR i.cause_racine ILIKE ? OR f.description_initiale ILIKE ?"

            );

        }

        return String.join(" OR ", clauses);

    }



    private static void appendLikeParams(List<Object> params, List<String> likePatterns) {

        for (String pattern : likePatterns) {

            params.add(pattern);

            params.add(pattern);

            params.add(pattern);

        }

    }



    private static void appendSymptomLikeParams(List<Object> params, List<String> likePatterns) {
        for (String pattern : likePatterns) {
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
    }

    private static void appendFullLikeParams(List<Object> params, List<String> likePatterns) {
        for (String pattern : likePatterns) {
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
    }

    @Override
    public List<SimilarIntervention> searchBySemanticContext(
            QuerySignals signals,
            SearchContext context,
            int topK
    ) {
        if (!signals.hasSemanticContext()
                || signals.symptomKeywords() == null
                || signals.symptomKeywords().isEmpty()) {
            return List.of();
        }

        List<String> keywords = signals.symptomKeywords().stream()
                .filter(k -> k != null && k.length() > 2)
                .distinct()
                .toList();
        if (keywords.isEmpty()) {
            return List.of();
        }

        StringBuilder keywordMatchSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0) {
                keywordMatchSql.append(" + ");
            }
            keywordMatchSql.append("""
                    (CASE WHEN i.symptomes ILIKE ? OR i.cause_racine ILIKE ?
                          OR f.description_initiale ILIKE ? OR f.code_defaut ILIKE ?
                          THEN 1 ELSE 0 END)
                    """);
            String pattern = "%" + keywords.get(i) + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        StringBuilder sql = new StringBuilder("""
                SELECT i.id AS intervention_id,
                       i.symptomes,
                       i.cause_racine,
                       i.actions_correctives,
                       i.analyse_technique,
                       e.code AS equipment_code,
                       e.id AS equipment_id,
                       e.famille AS equipment_family,
                       e.zone AS equipment_zone,
                       f.code_defaut AS fault_code,
                       f.description_initiale AS failure_description,
                       e.constructeur AS constructeur,
                       (0.88 + 0.04 * (%s)) AS base_similarity
                FROM interventions i
                JOIN failures f ON f.id = i.failure_id
                JOIN equipment e ON e.id = f.equipment_id
                WHERE i.statut_validation = 'VALIDEE'
                """.formatted(keywordMatchSql));

        signals.equipmentZone().ifPresent(zone -> {
            sql.append(" AND e.zone ILIKE ?");
            params.add(zone);
        });
        signals.equipmentFamily().ifPresent(family -> {
            sql.append(" AND e.famille ILIKE ?");
            params.add(family);
        });

        sql.append(" AND (");
        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0) {
                sql.append(" OR ");
            }
            sql.append("""
                    (i.symptomes ILIKE ? OR i.cause_racine ILIKE ?
                     OR f.description_initiale ILIKE ? OR f.code_defaut ILIKE ?)
                    """);
            String pattern = "%" + keywords.get(i) + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        sql.append(") ORDER BY base_similarity DESC LIMIT ?");
        params.add(Math.max(topK, topK * BOOST_OVERSAMPLE_FACTOR));

        List<SimilarIntervention> results = jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> mapRow(rs, context, signals),
                params.toArray()
        );

        if (results.size() > topK) {
            return results.subList(0, topK);
        }
        return results;
    }
}

