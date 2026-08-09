package com.ocp.eia.modules.knowledge.domain;

/**
 * Thrown when the LLM circuit is open or the provider call fails/times out.
 * Callers should map this to history/domain fallbacks — never treat as a successful completion.
 */
public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String message) {
        super(message);
    }

    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
