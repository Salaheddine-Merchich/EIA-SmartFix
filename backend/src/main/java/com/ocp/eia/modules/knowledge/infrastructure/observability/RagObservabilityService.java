package com.ocp.eia.modules.knowledge.infrastructure.observability;

import com.ocp.eia.modules.knowledge.infrastructure.ai.ResilientEmbeddingProvider;
import com.ocp.eia.modules.knowledge.infrastructure.ai.ResilientLlmProvider;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@Slf4j
public class RagObservabilityService {

    private final MeterRegistry meterRegistry;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<ResilientEmbeddingProvider> resilientEmbeddingProvider;
    private final ObjectProvider<ResilientLlmProvider> resilientLlmProvider;

    private final AtomicInteger indexedInterventionsCount = new AtomicInteger(0);
    private final AtomicInteger indexedDocumentsCount = new AtomicInteger(0);
    private final AtomicLong lastSuccessfulQuery = new AtomicLong(System.currentTimeMillis());
    private final AtomicInteger activeQueries = new AtomicInteger(0);
    private final Map<String, AtomicInteger> errorCounts = new HashMap<>();

    private final AtomicInteger lowConfidenceResponses = new AtomicInteger(0);
    private final AtomicInteger fallbackResponses = new AtomicInteger(0);
    private final AtomicInteger fastPathResponses = new AtomicInteger(0);

    public RagObservabilityService(
            MeterRegistry meterRegistry,
            JdbcTemplate jdbcTemplate,
            ObjectProvider<ResilientEmbeddingProvider> resilientEmbeddingProvider,
            ObjectProvider<ResilientLlmProvider> resilientLlmProvider) {
        this.meterRegistry = meterRegistry;
        this.jdbcTemplate = jdbcTemplate;
        this.resilientEmbeddingProvider = resilientEmbeddingProvider;
        this.resilientLlmProvider = resilientLlmProvider;
    }

    public void init() {
        Gauge.builder("rag.health.indexed_interventions", indexedInterventionsCount, AtomicInteger::get)
            .description("Nombre d'interventions indexées")
            .register(meterRegistry);

        Gauge.builder("rag.health.indexed_documents", indexedDocumentsCount, AtomicInteger::get)
            .description("Nombre de documents techniques indexés")
            .register(meterRegistry);

        Gauge.builder("rag.health.last_successful_query_age_seconds", this,
                obs -> (System.currentTimeMillis() - obs.lastSuccessfulQuery.get()) / 1000.0)
            .description("Âge en secondes de la dernière requête réussie")
            .register(meterRegistry);

        Gauge.builder("rag.health.active_queries", activeQueries, AtomicInteger::get)
            .description("Nombre de requêtes RAG en cours")
            .register(meterRegistry);

        Gauge.builder("rag.quality.low_confidence_responses", lowConfidenceResponses, AtomicInteger::get)
            .description("Nombre de réponses avec confiance faible")
            .register(meterRegistry);

        Gauge.builder("rag.quality.fallback_responses", fallbackResponses, AtomicInteger::get)
            .description("Nombre de réponses de secours utilisées")
            .register(meterRegistry);

        Gauge.builder("rag.quality.fast_path_responses", fastPathResponses, AtomicInteger::get)
            .description("Nombre de réponses fast path (sans LLM)")
            .register(meterRegistry);

        Gauge.builder("rag.circuit_breaker.embedding_failures", this,
                obs -> (double) obs.embeddingFailureCount())
            .description("Nombre d'échecs consecutifs embedding")
            .register(meterRegistry);

        Gauge.builder("rag.circuit_breaker.llm_failures", this,
                obs -> (double) obs.llmFailureCount())
            .description("Nombre d'échecs consecutifs LLM")
            .register(meterRegistry);

        log.info("Métriques RAG avancées initialisées");
    }

    @Scheduled(fixedRate = 60000)
    public void collectDatabaseMetrics() {
        try {
            Integer interventions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM intervention_embeddings", Integer.class);
            indexedInterventionsCount.set(interventions != null ? interventions : 0);

            Integer documents = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT document_id) FROM knowledge_document_embeddings", Integer.class);
            indexedDocumentsCount.set(documents != null ? documents : 0);

        } catch (Exception e) {
            log.warn("Erreur collecte métriques base de données: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 300000)
    public void checkAlerts() {
        long lastQueryAge = (System.currentTimeMillis() - lastSuccessfulQuery.get()) / 1000;
        if (lastQueryAge > 600) {
            log.error("ALERTE RAG: Aucune requête réussie depuis {} secondes", lastQueryAge);
        }

        int totalLowQuality = lowConfidenceResponses.get() + fallbackResponses.get();
        if (totalLowQuality > 50) {
            log.warn("ALERTE RAG QUALITÉ: {} réponses de faible qualité détectées", totalLowQuality);
        }

        int embeddingFailures = embeddingFailureCount();
        int llmFailures = llmFailureCount();
        if (embeddingFailures >= 5) {
            log.error("ALERTE RAG: Circuit breaker embedding en danger (failures: {})", embeddingFailures);
        }
        if (llmFailures >= 5) {
            log.error("ALERTE RAG: Circuit breaker LLM en danger (failures: {})", llmFailures);
        }

        if (indexedInterventionsCount.get() == 0 && indexedDocumentsCount.get() == 0) {
            log.error("ALERTE RAG: Base de connaissances complètement vide!");
        }
    }

    public void recordSuccessfulQuery() {
        lastSuccessfulQuery.set(System.currentTimeMillis());
    }

    public void incrementActiveQueries() {
        activeQueries.incrementAndGet();
    }

    public void decrementActiveQueries() {
        activeQueries.decrementAndGet();
    }

    public void recordLowConfidenceResponse() {
        lowConfidenceResponses.incrementAndGet();
    }

    public void recordFallbackResponse() {
        fallbackResponses.incrementAndGet();
    }

    public void recordFastPathResponse() {
        fastPathResponses.incrementAndGet();
    }

    public void recordError(String errorType) {
        errorCounts.computeIfAbsent(errorType, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public RagHealthReport getHealthReport() {
        ResilientEmbeddingProvider embedding = resilientEmbeddingProvider.getIfAvailable();
        ResilientLlmProvider llm = resilientLlmProvider.getIfAvailable();
        return RagHealthReport.builder()
            .timestamp(LocalDateTime.now())
            .indexedInterventions(indexedInterventionsCount.get())
            .indexedDocuments(indexedDocumentsCount.get())
            .activeQueries(activeQueries.get())
            .lastSuccessfulQueryAge((System.currentTimeMillis() - lastSuccessfulQuery.get()) / 1000)
            .lowConfidenceResponses(lowConfidenceResponses.get())
            .fallbackResponses(fallbackResponses.get())
            .embeddingCircuitBreakerState(embedding != null
                    ? String.valueOf(embedding.getCircuitBreakerState()) : "UNAVAILABLE")
            .embeddingFailures(embedding != null ? embedding.getFailureCount() : 0)
            .llmCircuitBreakerState(llm != null
                    ? String.valueOf(llm.getCircuitBreakerState()) : "UNAVAILABLE")
            .llmFailures(llm != null ? llm.getFailureCount() : 0)
            .errorCounts(new HashMap<>(errorCounts))
            .overallHealth(calculateOverallHealth())
            .build();
    }

    private String calculateOverallHealth() {
        if (embeddingFailureCount() >= 5 || llmFailureCount() >= 5) {
            return "CRITICAL";
        }

        if ((System.currentTimeMillis() - lastSuccessfulQuery.get()) > 600000) {
            return "WARNING";
        }

        if (indexedInterventionsCount.get() == 0 && indexedDocumentsCount.get() == 0) {
            return "WARNING";
        }

        return "HEALTHY";
    }

    private int embeddingFailureCount() {
        ResilientEmbeddingProvider provider = resilientEmbeddingProvider.getIfAvailable();
        return provider != null ? provider.getFailureCount() : 0;
    }

    private int llmFailureCount() {
        ResilientLlmProvider provider = resilientLlmProvider.getIfAvailable();
        return provider != null ? provider.getFailureCount() : 0;
    }

    @lombok.Builder
    @lombok.Data
    public static class RagHealthReport {
        private LocalDateTime timestamp;
        private int indexedInterventions;
        private int indexedDocuments;
        private int activeQueries;
        private long lastSuccessfulQueryAge;
        private int lowConfidenceResponses;
        private int fallbackResponses;
        private String embeddingCircuitBreakerState;
        private int embeddingFailures;
        private String llmCircuitBreakerState;
        private int llmFailures;
        private Map<String, AtomicInteger> errorCounts;
        private String overallHealth;
    }
}
