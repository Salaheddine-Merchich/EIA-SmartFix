package com.ocp.eia.modules.knowledge.domain.port;

import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;

import java.util.List;
import java.util.Optional;

/**
 * Recherche exacte par code défaut sur les interventions validées.
 */
public interface ExactFaultCodeSearchPort {

    List<SimilarIntervention> searchByExactCode(String faultCode, Optional<String> manufacturer, int topK);

    boolean existsFaultCode(String faultCode);
}
