package com.ocp.eia.application.validation;

import com.ocp.eia.modules.knowledge.application.AssistQueryValidator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidAssistQueryValidator implements ConstraintValidator<ValidAssistQuery, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return AssistQueryValidator.isValid(value);
    }
}
