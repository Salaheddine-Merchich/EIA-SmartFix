package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.knowledge.domain.model.QuerySignals;
import com.ocp.eia.modules.knowledge.domain.model.SearchContext;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RetrievalRerankerTest {

    @Test
    void rerank_exactMatchComesFirst() {
        UUID exactId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        SimilarIntervention exact = row(exactId, "E21", "Variateurs", null, 1.0);
        SimilarIntervention other = row(otherId, "E05", "Variateurs", null, 0.9);
        QuerySignals signals = signalsWithCode("E21");

        List<SimilarIntervention> reranked = RetrievalReranker.rerank(
                List.of(other, exact),
                List.of(exact),
                signals,
                SearchContext.none(),
                10.0,
                1.5,
                0.6
        );

        assertEquals(exactId, reranked.get(0).interventionId());
    }

    @Test
    void rerank_mismatchedCodeIsPenalized() {
        UUID e05 = UUID.randomUUID();
        UUID e21 = UUID.randomUUID();
        SimilarIntervention overload = row(e05, "E05", "Variateurs", null, 0.95);
        SimilarIntervention heat = row(e21, "E21", "Variateurs", null, 0.70);
        QuerySignals signals = signalsWithCode("E21");

        List<SimilarIntervention> reranked = RetrievalReranker.rerank(
                List.of(overload, heat),
                List.of(),
                signals,
                SearchContext.none(),
                10.0,
                1.5,
                0.6
        );

        assertEquals(e21, reranked.get(0).interventionId());
    }

    @Test
    void rerank_semanticQuery_boostsMatchingZone() {
        UUID pvId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        SimilarIntervention pv = row(pvId, null, "Pompe", "Station PV", 0.75);
        SimilarIntervention conv = row(convId, null, "Convoyeur", "Zone Convoyage", 0.80);

        QuerySignals signals = new QuerySignals(
                List.of(),
                Optional.empty(),
                Optional.of("pompe"),
                Optional.of("Pompe"),
                Optional.of("Station PV"),
                List.of("veille", "sommeil"),
                List.of("no_start")
        );
        SearchContext context = SearchContext.withSignals(
                null, null, "Pompe", "Station PV", null, List.of(), 2.0, 1.6, 1.8, 1.8
        );

        List<SimilarIntervention> reranked = RetrievalReranker.rerank(
                List.of(conv, pv),
                List.of(),
                signals,
                context,
                10.0,
                1.5,
                0.6
        );

        assertEquals(pvId, reranked.get(0).interventionId());
    }

    private static QuerySignals signalsWithCode(String code) {
        return new QuerySignals(
                List.of(code),
                Optional.of("Hitachi"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                List.of()
        );
    }

    private static SimilarIntervention row(
            UUID id,
            String faultCode,
            String family,
            String zone,
            double similarity
    ) {
        return new SimilarIntervention(
                id, "EQ-1", "Symptômes", "Cause", "Actions", "Analyse",
                similarity, faultCode, "Hitachi", family, zone
        );
    }
}
