package com.ocp.eia.modules.knowledge.infrastructure.ai;

import com.ocp.eia.config.AppProperties;
import com.ocp.eia.modules.knowledge.domain.port.EmbeddingProviderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Primary
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@ConditionalOnBean(CachedEmbeddingProvider.class)
@RequiredArgsConstructor
@Slf4j
public class ResilientEmbeddingProvider implements EmbeddingProviderPort {

    private final CachedEmbeddingProvider delegate;
    private final AppProperties appProperties;
    
    // Circuit breaker state
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicReference<LocalDateTime> lastFailureTime = new AtomicReference<>();
    private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;
    
    private enum CircuitBreakerState {
        CLOSED,    // Normal operation
        OPEN,      // Failing fast
        HALF_OPEN  // Testing if service recovered
    }

    @Override
    public float[] embed(String text) {
        // Check circuit breaker
        if (isCircuitOpen()) {
            throw new RuntimeException("Embedding service circuit breaker is OPEN - too many recent failures");
        }
        
        Duration timeout = parseTimeout(appProperties.getAi().getRag().getPerformance().getEmbeddingTimeout());
        
        try {
            CompletableFuture<float[]> future = CompletableFuture.supplyAsync(() -> delegate.embed(text));
            float[] result = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            
            // Success: reset circuit breaker
            onSuccess();
            return result;
            
        } catch (TimeoutException e) {
            log.error("Timeout embedding après {}ms (textLength={})", timeout.toMillis(),
                    text != null ? text.length() : 0);
            onFailure();
            throw new RuntimeException("Embedding timeout", e);
            
        } catch (Exception e) {
            log.error("Erreur embedding: {}", e.getMessage());
            onFailure();
            throw new RuntimeException("Embedding failed", e);
        }
    }
    
    private boolean isCircuitOpen() {
        int threshold = appProperties.getAi().getRag().getPerformance().getCircuitBreakerThreshold();
        Duration circuitTimeout = parseTimeout(appProperties.getAi().getRag().getPerformance().getCircuitBreakerTimeout());
        
        if (state == CircuitBreakerState.CLOSED) {
            return false;
        }
        
        if (state == CircuitBreakerState.OPEN) {
            LocalDateTime lastFailure = lastFailureTime.get();
            if (lastFailure != null && lastFailure.plus(circuitTimeout).isBefore(LocalDateTime.now())) {
                // Transition to HALF_OPEN to test recovery
                state = CircuitBreakerState.HALF_OPEN;
                log.info("Circuit breaker embedding: OPEN → HALF_OPEN (testing recovery)");
                return false;
            }
            return true;
        }
        
        // HALF_OPEN: allow one request to test
        return false;
    }
    
    private void onSuccess() {
        if (state != CircuitBreakerState.CLOSED) {
            log.info("Circuit breaker embedding: {} → CLOSED (service recovered)", state);
            state = CircuitBreakerState.CLOSED;
        }
        failureCount.set(0);
    }
    
    private void onFailure() {
        int failures = failureCount.incrementAndGet();
        lastFailureTime.set(LocalDateTime.now());
        
        int threshold = appProperties.getAi().getRag().getPerformance().getCircuitBreakerThreshold();
        
        if (failures >= threshold && state == CircuitBreakerState.CLOSED) {
            state = CircuitBreakerState.OPEN;
            log.warn("Circuit breaker embedding: CLOSED → OPEN (failures: {})", failures);
        } else if (state == CircuitBreakerState.HALF_OPEN) {
            state = CircuitBreakerState.OPEN;
            log.warn("Circuit breaker embedding: HALF_OPEN → OPEN (test failed)");
        }
    }
    
    private Duration parseTimeout(String timeoutStr) {
        try {
            if (timeoutStr.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(timeoutStr.substring(0, timeoutStr.length() - 1)));
            } else if (timeoutStr.endsWith("ms")) {
                return Duration.ofMillis(Long.parseLong(timeoutStr.substring(0, timeoutStr.length() - 2)));
            } else {
                return Duration.ofSeconds(Long.parseLong(timeoutStr));
            }
        } catch (Exception e) {
            log.warn("Invalid timeout format '{}', using default 5s", timeoutStr);
            return Duration.ofSeconds(5);
        }
    }
    
    /**
     * Getter pour les métriques/monitoring
     */
    public CircuitBreakerState getCircuitBreakerState() {
        return state;
    }
    
    public int getFailureCount() {
        return failureCount.get();
    }
}