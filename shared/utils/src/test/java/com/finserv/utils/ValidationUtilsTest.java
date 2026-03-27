package com.finserv.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    // --- Issue #1: isValidAmount must reject zero ---

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
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("0.001")));
    }

    // --- Issue #7: isValidAccountNumber structural checks ---

    @Test
    void isValidAccountNumber_rejectsAllIdenticalDigits() {
        assertFalse(ValidationUtils.isValidAccountNumber("00000000"));
        assertFalse(ValidationUtils.isValidAccountNumber("11111111"));
        assertFalse(ValidationUtils.isValidAccountNumber("999999999"));
    }

    @Test
    void isValidAccountNumber_acceptsValidNumber() {
        assertTrue(ValidationUtils.isValidAccountNumber("12345678"));
        assertTrue(ValidationUtils.isValidAccountNumber("200000001"));
    }

    @Test
    void isValidAccountNumber_rejectsNull() {
        assertFalse(ValidationUtils.isValidAccountNumber(null));
    }

    @Test
    void isValidAccountNumber_rejectsTooShort() {
        assertFalse(ValidationUtils.isValidAccountNumber("1234567"));
    }

    @Test
    void isValidAccountNumber_rejectsTooLong() {
        assertFalse(ValidationUtils.isValidAccountNumber("1234567890123"));
    }

    @Test
    void isValidAccountNumber_rejectsNonDigits() {
        assertFalse(ValidationUtils.isValidAccountNumber("1234abcd"));
    }
}
