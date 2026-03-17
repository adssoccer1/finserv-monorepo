package com.finserv.middleware;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #9 fix: sensitive field masking in LoggingMiddleware.
 */
class LoggingMiddlewareTest {

    // --- Happy path: card numbers are masked ---
    @Test
    void maskSensitiveFields_shouldMaskCardNumber() {
        String body = "{\"cardNumber\":\"4111111111111111\",\"amount\":\"100.00\"}";
        String masked = LoggingMiddleware.maskSensitiveFields(body);
        assertFalse(masked.contains("4111111111111111"));
        assertTrue(masked.contains("****-****-****-XXXX"));
        assertTrue(masked.contains("100.00")); // amount preserved
    }

    // --- Happy path: account numbers show only last 4 digits ---
    @Test
    void maskSensitiveFields_shouldMaskAccountNumbers() {
        String body = "{\"sourceAccountId\":\"100000001\",\"destinationAccountId\":\"200000002\"}";
        String masked = LoggingMiddleware.maskSensitiveFields(body);
        assertFalse(masked.contains("100000001"));
        assertFalse(masked.contains("200000002"));
        assertTrue(masked.contains("***0001"));
        assertTrue(masked.contains("***0002"));
    }

    // --- Happy path: passwords are redacted ---
    @Test
    void maskSensitiveFields_shouldRedactPasswords() {
        String body = "{\"currentPassword\":\"secretPass123\",\"newPassword\":\"newSecret456\"}";
        String masked = LoggingMiddleware.maskSensitiveFields(body);
        assertFalse(masked.contains("secretPass123"));
        assertFalse(masked.contains("newSecret456"));
        assertTrue(masked.contains("[REDACTED]"));
    }

    // --- Happy path: tokens are redacted ---
    @Test
    void maskSensitiveFields_shouldRedactTokens() {
        String body = "{\"accessToken\":\"eyJhbGciOiJIUzI1NiJ9.xxx\",\"refreshToken\":\"eyJhbGciOiJIUzI1NiJ9.yyy\"}";
        String masked = LoggingMiddleware.maskSensitiveFields(body);
        assertFalse(masked.contains("eyJhbGciOiJIUzI1NiJ9"));
        assertTrue(masked.contains("[REDACTED]"));
    }

    // --- Edge case: null and empty bodies ---
    @Test
    void maskSensitiveFields_shouldHandleNullAndEmpty() {
        assertNull(LoggingMiddleware.maskSensitiveFields(null));
        assertEquals("", LoggingMiddleware.maskSensitiveFields(""));
    }

    // --- Edge case: body without sensitive fields should be unchanged ---
    @Test
    void maskSensitiveFields_shouldNotModifyNonSensitiveBody() {
        String body = "{\"userId\":\"user-001\",\"message\":\"Hello\"}";
        String masked = LoggingMiddleware.maskSensitiveFields(body);
        assertEquals(body, masked);
    }
}
