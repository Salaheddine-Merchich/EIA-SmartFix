package com.ocp.eia.presentation.exception;

import com.ocp.eia.application.dto.CommonDto.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleGeneric_returnsSafeInternalMessage() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleGeneric(new RuntimeException("secret stack detail"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ApiErrorResponse body = response.getBody();
        assertEquals("INTERNAL_ERROR", body.code());
        assertEquals("Une erreur interne est survenue", body.message());
        assertNotEquals("secret stack detail", body.message());
        assertNull(body.details());
    }
}
