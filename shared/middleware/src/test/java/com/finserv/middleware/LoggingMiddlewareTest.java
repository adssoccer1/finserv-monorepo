package com.finserv.middleware;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class LoggingMiddlewareTest {

    // === Issue #9: PII masking in request body logging ===

    @Test
    @DisplayName("#9 happy path: cardNumber is masked")
    void maskSensitiveFields_cardNumber_isMasked() {
        String body = "{\"cardNumber\":\"4111111111111111\",\"amount\":\"100.00\"}";
        String masked = LoggingMiddleware.maskSensitiveFields(body);
        assertFalse(masked.contains("4111111111111111"));
        assertTrue(masked.contains("\"cardNumber\":\"***\""));
        assertTrue(masked.contains("\"amount\":\"100.00\""));
    }

    @Test
    @DisplayName("#9 rejection: password field must not appear in logs")
    void maskSensitiveFields_password_isMasked() {
        String body = "{\"password\":\"s3cretP@ss!\",\"email\":\"alice@example.com\"}";
        String masked = LoggingMiddleware.maskSensitiveFields(body);
        assertFalse(masked.contains("s3cretP@ss!"));
        assertTrue(masked.contains("\"password\":\"***\""));
        assertTrue(masked.contains("alice@example.com"));
    }

    @Test
    @DisplayName("#9 edge: multiple sensitive fields masked simultaneously")
    void maskSensitiveFields_multipleFields_allMasked() {
        String body = "{\"cardNumber\":\"4111111111111111\",\"accountNumber\":\"12345678\",\"password\":\"secret\"}";
        String masked = LoggingMiddleware.maskSensitiveFields(body);
        assertFalse(masked.contains("4111111111111111"));
        assertFalse(masked.contains("12345678"));
        assertFalse(masked.contains("secret"));
    }

    @Test
    @DisplayName("#9 edge: null and empty body handled gracefully")
    void maskSensitiveFields_nullOrEmpty_returnsAsIs() {
        assertNull(LoggingMiddleware.maskSensitiveFields(null));
        assertEquals("", LoggingMiddleware.maskSensitiveFields(""));
    }

    @Test
    @DisplayName("#9 edge: body without sensitive fields remains unchanged")
    void maskSensitiveFields_noSensitiveFields_unchanged() {
        String body = "{\"amount\":\"100.00\",\"currency\":\"USD\"}";
        assertEquals(body, LoggingMiddleware.maskSensitiveFields(body));
    }
}
