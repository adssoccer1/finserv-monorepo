package com.finserv.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    // === Issue #1: isValidAmount must reject zero ===

    @Test
    @DisplayName("#1 happy path: positive amount with 2 decimals is valid")
    void isValidAmount_positiveAmount_returnsTrue() {
        assertTrue(ValidationUtils.isValidAmount(new BigDecimal("100.50")));
    }

    @Test
    @DisplayName("#1 rejection: zero amount is rejected")
    void isValidAmount_zero_returnsFalse() {
        assertFalse(ValidationUtils.isValidAmount(BigDecimal.ZERO));
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("0.00")));
    }

    @Test
    @DisplayName("#1 edge: negative amount is rejected")
    void isValidAmount_negative_returnsFalse() {
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("-1.00")));
    }

    @Test
    @DisplayName("#1 edge: null amount is rejected")
    void isValidAmount_null_returnsFalse() {
        assertFalse(ValidationUtils.isValidAmount(null));
    }

    @Test
    @DisplayName("#1 edge: more than 2 decimal places is rejected")
    void isValidAmount_tooManyDecimals_returnsFalse() {
        assertFalse(ValidationUtils.isValidAmount(new BigDecimal("10.123")));
    }

    // === Issue #7: Account number validation with Luhn checksum ===

    @Test
    @DisplayName("#7 happy path: valid Luhn account number accepted")
    void isValidAccountNumber_validLuhn_returnsTrue() {
        assertTrue(ValidationUtils.isValidAccountNumber("12345674"));   // 8 digits, passes Luhn
        assertTrue(ValidationUtils.isValidAccountNumber("49927398716")); // 11 digits, passes Luhn
    }

    @Test
    @DisplayName("#7 rejection: all-identical digits rejected")
    void isValidAccountNumber_allSameDigits_returnsFalse() {
        assertFalse(ValidationUtils.isValidAccountNumber("00000000"));
        assertFalse(ValidationUtils.isValidAccountNumber("11111111"));
        assertFalse(ValidationUtils.isValidAccountNumber("999999999"));
    }

    @Test
    @DisplayName("#7 rejection: fails Luhn checksum")
    void isValidAccountNumber_failsLuhn_returnsFalse() {
        assertFalse(ValidationUtils.isValidAccountNumber("12345678"));
    }

    @Test
    @DisplayName("#7 edge: null and blank rejected")
    void isValidAccountNumber_nullOrBlank_returnsFalse() {
        assertFalse(ValidationUtils.isValidAccountNumber(null));
        assertFalse(ValidationUtils.isValidAccountNumber(""));
        assertFalse(ValidationUtils.isValidAccountNumber("   "));
    }

    @Test
    @DisplayName("#7 edge: too short or too long rejected")
    void isValidAccountNumber_wrongLength_returnsFalse() {
        assertFalse(ValidationUtils.isValidAccountNumber("1234567"));   // 7 digits
        assertFalse(ValidationUtils.isValidAccountNumber("1234567890123")); // 13 digits
    }

    @Test
    @DisplayName("#7 Luhn algorithm: known test vectors")
    void passesLuhnCheck_knownVectors() {
        assertTrue(ValidationUtils.passesLuhnCheck("49927398716"));
        assertTrue(ValidationUtils.passesLuhnCheck("12345674"));
        assertFalse(ValidationUtils.passesLuhnCheck("49927398717"));
        assertFalse(ValidationUtils.passesLuhnCheck("12345678"));
    }
}
