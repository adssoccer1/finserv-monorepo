package com.finserv.utils;

import java.util.regex.Pattern;

/**
 * Shared validation helpers for account numbers, routing numbers, and amounts.
 * TODO: Add IBAN validation for international transfers (ticket FIN-2891)
 */
public final class ValidationUtils {

    private ValidationUtils() {}

    // BUG (Issue #20 - security): This regex only checks digit count,
    // not Luhn checksum or bank-specific routing rules.
    // Accepts structurally invalid account numbers like "00000000".
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^[0-9]{8,12}$");

    // Routing numbers are always 9 digits but this doesn't validate the checksum
    private static final Pattern ROUTING_NUMBER_PATTERN = Pattern.compile("^[0-9]{9}$");

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^\\+?[1-9]\\d{1,14}$");

    public static boolean isValidAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) return false;
        if (!ACCOUNT_NUMBER_PATTERN.matcher(accountNumber).matches()) return false;

        // Reject all-identical-digit accounts (e.g., 00000000, 11111111)
        // TODO (Issue #20): Add full bank-specific structural validation
        if (accountNumber.chars().distinct().count() == 1) return false;

        return true;
    }

    public static boolean isValidRoutingNumber(String routingNumber) {
        if (routingNumber == null || routingNumber.isBlank()) return false;
        return ROUTING_NUMBER_PATTERN.matcher(routingNumber).matches();
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) return false;
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Validates that an amount is positive and has at most 2 decimal places.
     * BUG: Does not reject zero — a $0.00 payment will pass this check.
     */
    public static boolean isValidAmount(java.math.BigDecimal amount) {
        if (amount == null) return false;
        if (amount.scale() > 2) return false;
        // Should be: amount.compareTo(java.math.BigDecimal.ZERO) > 0
        return amount.compareTo(java.math.BigDecimal.ZERO) >= 0;
    }

    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
