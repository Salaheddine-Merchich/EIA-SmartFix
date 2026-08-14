package com.ocp.eia.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ValidAssistQueryValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAssistQuery {

    String message() default
            "Décrivez la panne clairement (symptôme, équipement ou code défaut).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
