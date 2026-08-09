package com.ocp.eia.modules.knowledge.infrastructure.ai;

import com.ocp.eia.config.AppProperties;
import com.ocp.eia.modules.knowledge.domain.LlmUnavailableException;
import com.ocp.eia.modules.knowledge.domain.port.LlmProviderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

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
@ConditionalOnBean(OllamaLlmAdapter.class)
@RequiredArgsConstructor
@Slf4j
public class ResilientLlmProvider implements LlmProviderPort {

    private final OllamaLlmAdapter delegate;
    private final AppProperties appProperties;

    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicReference<LocalDateTime> lastFailureTime = new AtomicReference<>();
    private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;

    private enum CircuitBreakerState {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (isCircuitOpen()) {
            throw new LlmUnavailableException("Circuit breaker LLM ouvert");
        }

        Duration timeout = parseTimeout(appProperties.getAi().getRag().getPerformance().getLlmTimeout());

        try {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() ->
                    delegate.complete(systemPrompt, userPrompt));
            String result = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);

            onSuccess();
            return result;

        } catch (TimeoutException e) {
            log.error("Timeout LLM après {}ms", timeout.toMillis());
            onFailure();
            throw new LlmUnavailableException("Timeout LLM après " + timeout.toMillis() + "ms", e);

        } catch (LlmUnavailableException e) {
            throw e;

        } catch (Exception e) {
            log.error("Erreur LLM: {}", e.getMessage());
            onFailure();
            throw new LlmUnavailableException("Erreur LLM: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> stream(String systemPrompt, String userPrompt) {
        if (isCircuitOpen()) {
            return Flux.error(new LlmUnavailableException("Circuit breaker LLM ouvert"));
        }

        return delegate.stream(systemPrompt, userPrompt)
                .timeout(parseTimeout(appProperties.getAi().getRag().getPerformance().getLlmTimeout()))
                .doOnComplete(this::onSuccess)
                .doOnError(error -> {
                    log.error("Erreur streaming LLM: {}", error.getMessage());
                    onFailure();
                })
                .onErrorMap(error ->
                        error instanceof LlmUnavailableException
                                ? error
                                : new LlmUnavailableException("Erreur streaming LLM: " + error.getMessage(), error));
    }

    private boolean isCircuitOpen() {
        Duration circuitTimeout = parseTimeout(appProperties.getAi().getRag().getPerformance().getCircuitBreakerTimeout());

        if (state == CircuitBreakerState.CLOSED) {
            return false;
        }

        if (state == CircuitBreakerState.OPEN) {
            LocalDateTime lastFailure = lastFailureTime.get();
            if (lastFailure != null && lastFailure.plus(circuitTimeout).isBefore(LocalDateTime.now())) {
                state = CircuitBreakerState.HALF_OPEN;
                log.info("Circuit breaker LLM: OPEN → HALF_OPEN (testing recovery)");
                return false;
            }
            return true;
        }

        return false;
    }

    private void onSuccess() {
        if (state != CircuitBreakerState.CLOSED) {
            log.info("Circuit breaker LLM: {} → CLOSED (service recovered)", state);
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
            log.warn("Circuit breaker LLM: CLOSED → OPEN (failures: {})", failures);
        } else if (state == CircuitBreakerState.HALF_OPEN) {
            state = CircuitBreakerState.OPEN;
            log.warn("Circuit breaker LLM: HALF_OPEN → OPEN (test failed)");
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
            log.warn("Invalid timeout format '{}', using default 10s", timeoutStr);
            return Duration.ofSeconds(10);
        }
    }

    public CircuitBreakerState getCircuitBreakerState() {
        return state;
    }

    public int getFailureCount() {
        return failureCount.get();
    }
}
