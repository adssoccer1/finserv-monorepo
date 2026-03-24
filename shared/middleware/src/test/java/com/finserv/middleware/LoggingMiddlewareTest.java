package com.finserv.middleware;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoggingMiddlewareTest {

    @Test
    void maskSensitiveFields_masksCardNumber() {
        String body = "{\"cardNumber\":\"4111111111111111\",\"amount\":\"100.00\"}";
        String masked = LoggingMiddleware.maskSensitiveFields(body);

        assertFalse(masked.contains("4111111111111111"), "Card number must be masked");
        assertTrue(masked.contains("***1111"), "Last 4 digits should be preserved");
        assertTrue(masked.contains("\"amount\":\"100.00\""), "Non-sensitive fields must not be masked");
    }

    @Test
    void maskSensitiveFields_masksMultipleSensitiveFields() {
        String body = "{\"accountNumber\":\"200000001\",\"password\":\"s3cret!\",\"routingNumber\":\"021000021\"}";
        String masked = LoggingMiddleware.maskSensitiveFields(body);

        assertFalse(masked.contains("200000001"), "Account number must be masked");
        assertFalse(masked.contains("s3cret!"), "Password must be masked");
        assertFalse(masked.contains("021000021"), "Routing number must be masked");
        assertTrue(masked.contains("***0001"), "Last 4 of account number preserved");
        assertTrue(masked.contains("***ret!"), "Last 4 of password preserved");
    }

    @Test
    void maskSensitiveFields_masksTokenFields() {
        String body = "{\"accessToken\":\"eyJhbGciOiJIUzI1NiJ9.abc.def\",\"refreshToken\":\"eyJhbGciOiJIUzI1NiJ9.xyz\"}";
        String masked = LoggingMiddleware.maskSensitiveFields(body);

        assertFalse(masked.contains("eyJhbGciOiJIUzI1NiJ9"), "JWT tokens must be masked");
    }

    @Test
    void maskSensitiveFields_handlesShortValues() {
        String body = "{\"cvv\":\"123\"}";
        String masked = LoggingMiddleware.maskSensitiveFields(body);

        assertFalse(masked.contains("123"), "Short CVV must be fully masked");
        assertTrue(masked.contains("***"), "Short values should show *** only");
    }

    @Test
    void maskSensitiveFields_handlesEmptyAndNullBody() {
        assertEquals("", LoggingMiddleware.maskSensitiveFields(""));
        assertEquals("  ", LoggingMiddleware.maskSensitiveFields("  "));
        assertNull(LoggingMiddleware.maskSensitiveFields(null));
    }

    @Test
    void maskSensitiveFields_doesNotMaskNonSensitiveFields() {
        String body = "{\"amount\":\"500.00\",\"currency\":\"USD\",\"memo\":\"Rent payment\"}";
        String masked = LoggingMiddleware.maskSensitiveFields(body);

        assertEquals(body, masked, "Non-sensitive fields must pass through unchanged");
    }

    @Test
    void maskSensitiveFields_handlesCamelAndSnakeCaseVariants() {
        String body = "{\"card_number\":\"4111111111111111\",\"account_number\":\"200000001\"}";
        String masked = LoggingMiddleware.maskSensitiveFields(body);

        assertFalse(masked.contains("4111111111111111"), "snake_case card_number must be masked");
        assertFalse(masked.contains("200000001"), "snake_case account_number must be masked");
    }
}
