package com.finserv.middleware;

import com.finserv.utils.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #20 fix: stack traces and class names must not leak in error responses.
 */
class ErrorHandlerTest {

    private final ErrorHandler errorHandler = new ErrorHandler();

    // --- Happy path: generic error returns safe message ---
    @Test
    void handleGenericError_shouldReturnGenericMessage() {
        RuntimeException exception = new RuntimeException(
            "NullPointerException at com.finserv.payments.PaymentRepository.debitAccount");

        ResponseEntity<Map<String, Object>> response = errorHandler.handleGenericError(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCodes.INTERNAL_ERROR, body.get("code"));

        // Must NOT contain internal details
        String message = (String) body.get("message");
        assertFalse(message.contains("NullPointerException"));
        assertFalse(message.contains("com.finserv"));
        assertFalse(message.contains("PaymentRepository"));
    }

    // --- Rejection: exceptionType field must not be present ---
    @Test
    void handleGenericError_shouldNotExposeExceptionType() {
        Exception exception = new IllegalStateException("DB connection pool exhausted");

        ResponseEntity<Map<String, Object>> response = errorHandler.handleGenericError(exception);

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.containsKey("exceptionType"));
    }

    // --- Edge case: SQL error message must not leak ---
    @Test
    void handleGenericError_shouldNotExposeSqlErrors() {
        Exception exception = new RuntimeException(
            "ERROR: relation \"payments\" does not exist at character 15");

        ResponseEntity<Map<String, Object>> response = errorHandler.handleGenericError(exception);

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        String message = (String) body.get("message");
        assertFalse(message.contains("relation"));
        assertFalse(message.contains("payments"));
        assertTrue(message.contains("unexpected error"));
    }

    // --- Validation errors should still return the specific message ---
    @Test
    void handleValidationError_shouldReturnSpecificMessage() {
        IllegalArgumentException exception = new IllegalArgumentException("Amount must be positive");

        ResponseEntity<Map<String, Object>> response = errorHandler.handleValidationError(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Amount must be positive", body.get("message"));
    }
}
