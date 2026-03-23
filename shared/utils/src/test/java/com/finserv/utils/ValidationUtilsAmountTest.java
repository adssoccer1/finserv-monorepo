package com.finserv.utils;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ValidationUtils.isValidAmount() — fixes Issue #1.
 */
class ValidationUtilsAmountTest {

    // Happy path: valid positive amounts
    @Test
    void validPositiveAmount_returnsTrue() {
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("10.00")));
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("0.01")));
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("49999.99")));
        assertTrue(ValidationUtils.isValidAmount(BigDecimal.ONE));
    }

    // Rejection case: zero amount must be rejected (the bug fix)
    @Test
    void zeroAmount_returnsFalse() {
        assertFalse(ValidationUtils.isValidAmount(BigDecimal.ZERO));
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("0.00")));
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("0")));
    }

    // Rejection case: negative amounts
    @Test
    void negativeAmount_returnsFalse() {
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("-1.00")));
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("-0.01")));
    }

    // Edge case: null amount
    @Test
    void nullAmount_returnsFalse() {
        assertFalse(ValidationUtils.isValidAmount(null));
    }

    // Edge case: more than 2 decimal places
    @Test
    void tooManyDecimalPlaces_returnsFalse() {
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("10.001")));
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("0.999")));
    }

    // Edge case: exactly 2 decimal places
    @Test
    void exactlyTwoDecimalPlaces_returnsTrue() {
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("1.50")));
    }
}
