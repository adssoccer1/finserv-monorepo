package com.finserv.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    // --- isValidAmount tests (Issue #1) ---

    @Test
    void isValidAmount_rejectsZero() {
        assertFalse(ValidationUtils.isValidAmount(BigDecimal.ZERO));
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("0.00")));
    }

    @Test
    void isValidAmount_acceptsPositiveAmount() {
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("0.01")));
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("100.00")));
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("49999.99")));
    }

    @Test
    void isValidAmount_rejectsNegativeAmount() {
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("-1.00")));
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("-0.01")));
    }

    @Test
    void isValidAmount_rejectsNull() {
        assertFalse(ValidationUtils.isValidAmount(null));
    }

    @Test
    void isValidAmount_rejectsMoreThanTwoDecimalPlaces() {
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("10.123")));
    }

    @Test
    void isValidAmount_acceptsTwoDecimalPlaces() {
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("10.12")));
    }
}
