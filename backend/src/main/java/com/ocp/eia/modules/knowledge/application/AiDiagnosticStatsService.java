package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.application.dto.DashboardDto.AiReliabilityStats;
import com.ocp.eia.modules.knowledge.domain.model.AiDiagnosticTrace;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

@Service
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
public class AiDiagnosticStatsService {

    private final AtomicLong diagnosticsCount = new AtomicLong();
    private final AtomicLong retrievalCount = new AtomicLong();
    private final DoubleAdder confidenceSum = new DoubleAdder();

    public void record(AiDiagnosticTrace trace) {
        diagnosticsCount.incrementAndGet();
        retrievalCount.addAndGet(trace.filteredCount());
        confidenceSum.add(trace.confidenceScore());
    }

    public Optional<AiReliabilityStats> snapshot() {
        long count = diagnosticsCount.get();
        if (count == 0) {
            return Optional.empty();
        }
        double avgConfidence = Math.round((confidenceSum.sum() / count) * 10.0) / 10.0;
        return Optional.of(new AiReliabilityStats(count, avgConfidence, retrievalCount.get()));
    }
}
