package com.ocp.eia.shared.exception;

public class DomainRuleViolationException extends RuntimeException {

    private final ViolationType type;

    public DomainRuleViolationException(ViolationType type, String message) {
        super(message);
        this.type = type;
    }

    public ViolationType getType() {
        return type;
    }

    public enum ViolationType {
        BAD_REQUEST,
        FORBIDDEN,
        CONFLICT
    }
}
