package com.finserv.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    // --- isValidAmount: happy path ---
    @Test
    void isValidAmount_positiveAmount_returnsTrue() {
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("100.00")));
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("0.01")));
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("49999.99")));
    }

    // --- isValidAmount: rejection cases ---
    @Test
    void isValidAmount_zero_returnsFalse() {
        assertFalse(ValidationUtils.isValidAmount(BigDecimal.ZERO));
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("0.00")));
    }

    @Test
    void isValidAmount_negative_returnsFalse() {
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("-1.00")));
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("-0.01")));
    }

    @Test
    void isValidAmount_null_returnsFalse() {
        assertFalse(ValidationUtils.isValidAmount(null));
    }

    // --- isValidAmount: edge cases ---
    @Test
    void isValidAmount_moreThanTwoDecimalPlaces_returnsFalse() {
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("10.123")));
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("0.001")));
    }

    @Test
    void isValidAmount_smallestValidAmount_returnsTrue() {
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("0.01")));
    }
}
