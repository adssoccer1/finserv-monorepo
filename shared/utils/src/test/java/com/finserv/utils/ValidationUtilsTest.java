package com.finserv.utils;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    // Happy path: valid positive amounts are accepted
    @Test
    void isValidAmount_positiveAmount_returnsTrue() {
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("10.00")));
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("0.01")));
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("49999.99")));
    }

    // Rejection: zero amount must be rejected
    @Test
    void isValidAmount_zeroAmount_returnsFalse() {
        assertFalse(ValidationUtils.isValidAmount(BigDecimal.ZERO));
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("0.00")));
    }

    // Rejection: negative amounts must be rejected
    @Test
    void isValidAmount_negativeAmount_returnsFalse() {
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("-1.00")));
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("-0.01")));
    }

    // Edge case: null amount must be rejected
    @Test
    void isValidAmount_nullAmount_returnsFalse() {
        assertFalse(ValidationUtils.isValidAmount(null));
    }

    // Edge case: more than 2 decimal places must be rejected
    @Test
    void isValidAmount_tooManyDecimalPlaces_returnsFalse() {
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("10.001")));
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("1.999")));
    }

    // Edge case: whole numbers are valid (scale 0)
    @Test
    void isValidAmount_wholeNumber_returnsTrue() {
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("100")));
        assertTrue(ValidationUtils.isValidAmount(BigDecimal.ONE));
    }
}
