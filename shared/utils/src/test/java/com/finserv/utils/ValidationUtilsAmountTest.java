package com.finserv.utils;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ValidationUtils.isValidAmount() — verifies that zero-amount
 * payments are rejected (fix for Issue #1).
 */
class ValidationUtilsAmountTest {

    // Happy path: valid positive amounts
    @Test
    void validAmount_positiveWithTwoDecimals() {
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("100.50")));
    }

    @Test
    void validAmount_positiveWholeNumber() {
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("1")));
    }

    @Test
    void validAmount_smallPositive() {
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("0.01")));
    }

    // Rejection: zero amount must be rejected
    @Test
    void invalidAmount_zero() {
        assertFalse(ValidationUtils.isValidAmount(BigDecimal.ZERO));
    }

    @Test
    void invalidAmount_zeroWithScale() {
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("0.00")));
    }

    // Edge cases
    @Test
    void invalidAmount_negative() {
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("-10.00")));
    }

    @Test
    void invalidAmount_null() {
        assertFalse(ValidationUtils.isValidAmount(null));
    }

    @Test
    void invalidAmount_tooManyDecimals() {
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("10.123")));
    }

    @Test
    void validAmount_maxReasonable() {
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("99999.99")));
    }
}
