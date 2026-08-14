package com.ocp.eia.modules.knowledge.infrastructure.persistence;

import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import com.ocp.eia.modules.knowledge.domain.port.ExactFaultCodeSearchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@RequiredArgsConstructor
public class PgExactFaultCodeSearchAdapter implements ExactFaultCodeSearchPort {

    private static final double EXACT_MATCH_SIMILARITY = 1.0;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<SimilarIntervention> searchByExactCode(String faultCode, Optional<String> manufacturer, int topK) {
        if (faultCode == null || faultCode.isBlank()) {
            return List.of();
        }

        StringBuilder sql = new StringBuilder("""
                SELECT i.id AS intervention_id,
                       i.symptomes,
                       i.cause_racine,
                       i.actions_correctives,
                       i.analyse_technique,
                       e.code AS equipment_code,
                       f.code_defaut AS fault_code,
                       e.constructeur AS constructeur
                FROM interventions i
                JOIN failures f ON f.id = i.failure_id
                JOIN equipment e ON e.id = f.equipment_id
                WHERE i.statut_validation = 'VALIDEE'
                  AND UPPER(f.code_defaut) = UPPER(?)
                """);

        List<Object> params = new ArrayList<>();
        params.add(faultCode.trim());

        if (manufacturer.isPresent() && !manufacturer.get().isBlank()) {
            sql.append(" AND e.constructeur ILIKE ?");
            params.add("%" + manufacturer.get().trim() + "%");
        }

        sql.append(" ORDER BY i.created_at DESC NULLS LAST LIMIT ?");
        params.add(topK);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new SimilarIntervention(
                UUID.fromString(rs.getString("intervention_id")),
                rs.getString("equipment_code"),
                rs.getString("symptomes"),
                rs.getString("cause_racine"),
                rs.getString("actions_correctives"),
                rs.getString("analyse_technique"),
                EXACT_MATCH_SIMILARITY,
                rs.getString("fault_code"),
                rs.getString("constructeur")
        ), params.toArray());
    }

    @Override
    public boolean existsFaultCode(String faultCode) {
        if (faultCode == null || faultCode.isBlank()) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM failures f
                JOIN interventions i ON i.failure_id = f.id
                WHERE i.statut_validation = 'VALIDEE'
                  AND UPPER(f.code_defaut) = UPPER(?)
                """, Integer.class, faultCode.trim());
        return count != null && count > 0;
    }
}
