package com.ocp.eia.modules.knowledge.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
public class RagRetrievalMetrics {

    private final Counter vectorCount;
    private final Counter textCount;
    private final Counter mergedCount;
    private final Counter filteredCount;
    private final Counter llmCalls;
    private final Timer duration;

    public RagRetrievalMetrics(MeterRegistry meterRegistry) {
        this.vectorCount = Counter.builder("rag.retrieval.vector.count")
                .description("Nombre de résultats retournés par la recherche vectorielle")
                .register(meterRegistry);
        this.textCount = Counter.builder("rag.retrieval.text.count")
                .description("Nombre de résultats retournés par la recherche texte")
                .register(meterRegistry);
        this.mergedCount = Counter.builder("rag.retrieval.merged.count")
                .description("Nombre de résultats après fusion hybride")
                .register(meterRegistry);
        this.filteredCount = Counter.builder("rag.retrieval.filtered.count")
                .description("Nombre de résultats retenus après filtrage par seuil")
                .register(meterRegistry);
        this.llmCalls = Counter.builder("rag.retrieval.llm.calls")
                .description("Nombre d'appels LLM déclenchés par le RAG")
                .register(meterRegistry);
        this.duration = Timer.builder("rag.retrieval.duration")
                .description("Durée du pipeline de retrieval RAG (hors LLM)")
                .register(meterRegistry);
    }

    public Timer.Sample startRetrievalTimer() {
        return Timer.start();
    }

    public void recordRetrievalDuration(Timer.Sample sample) {
        sample.stop(duration);
    }

    public void recordVectorCount(int count) {
        vectorCount.increment(count);
    }

    public void recordTextCount(int count) {
        textCount.increment(count);
    }

    public void recordMergedCount(int count) {
        mergedCount.increment(count);
    }

    public void recordFilteredCount(int count) {
        filteredCount.increment(count);
    }

    public void recordLlmCall() {
        llmCalls.increment();
    }
}
