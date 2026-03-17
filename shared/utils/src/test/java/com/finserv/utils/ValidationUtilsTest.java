package com.finserv.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #7 fix: account number structural validation.
 */
class ValidationUtilsTest {

    // --- Happy path: valid account numbers ---
    @Test
    void isValidAccountNumber_shouldAcceptValidNumbers() {
        assertTrue(ValidationUtils.isValidAccountNumber("12345678"));     // 8 digits
        assertTrue(ValidationUtils.isValidAccountNumber("123456789012")); // 12 digits
        assertTrue(ValidationUtils.isValidAccountNumber("100000001"));    // existing test account
        assertTrue(ValidationUtils.isValidAccountNumber("200000001"));    // existing test account
    }

    // --- Rejection: all-identical-digit numbers ---
    @Test
    void isValidAccountNumber_shouldRejectAllIdenticalDigits() {
        assertFalse(ValidationUtils.isValidAccountNumber("00000000"));
        assertFalse(ValidationUtils.isValidAccountNumber("11111111"));
        assertFalse(ValidationUtils.isValidAccountNumber("999999999"));
        assertFalse(ValidationUtils.isValidAccountNumber("111111111111"));
    }

    // --- Edge cases: null, blank, wrong length ---
    @Test
    void isValidAccountNumber_shouldRejectNullAndBlank() {
        assertFalse(ValidationUtils.isValidAccountNumber(null));
        assertFalse(ValidationUtils.isValidAccountNumber(""));
        assertFalse(ValidationUtils.isValidAccountNumber("   "));
    }

    @Test
    void isValidAccountNumber_shouldRejectWrongLength() {
        assertFalse(ValidationUtils.isValidAccountNumber("1234567"));      // 7 digits - too short
        assertFalse(ValidationUtils.isValidAccountNumber("1234567890123")); // 13 digits - too long
    }

    @Test
    void isValidAccountNumber_shouldRejectNonDigits() {
        assertFalse(ValidationUtils.isValidAccountNumber("1234abcd"));
        assertFalse(ValidationUtils.isValidAccountNumber("12-34-5678"));
    }

    // --- isValidAmount: verify positive amounts accepted and negatives rejected ---
    @Test
    void isValidAmount_shouldAcceptPositiveAmounts() {
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("0.01")));
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("100.00")));
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("50000")));
    }

    @Test
    void isValidAmount_shouldRejectNegativeAmounts() {
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("-1.00")));
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("-0.01")));
    }
}
