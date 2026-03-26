package com.finserv.utils;

import java.util.regex.Pattern;

/**
 * Shared validation helpers for account numbers, routing numbers, and amounts.
 * TODO: Add IBAN validation for international transfers (ticket FIN-2891)
 */
public final class ValidationUtils {

    private ValidationUtils() {}

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
        if (isAllIdenticalDigits(accountNumber)) return false;
        return passesLuhnCheck(accountNumber);
    }

    /**
     * Luhn checksum validation (ISO/IEC 7812-1).
     */
    static boolean passesLuhnCheck(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = number.charAt(i) - '0';
            if (alternate) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private static boolean isAllIdenticalDigits(String s) {
        char first = s.charAt(0);
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) != first) return false;
        }
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
     * Validates that an amount is strictly positive and has at most 2 decimal places.
     */
    public static boolean isValidAmount(java.math.BigDecimal amount) {
        if (amount == null) return false;
        if (amount.scale() > 2) return false;
        return amount.compareTo(java.math.BigDecimal.ZERO) > 0;
    }

    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
