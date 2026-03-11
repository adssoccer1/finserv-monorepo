package com.finserv.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;

/**
 * Utility class for currency formatting and arithmetic.
 * All monetary values should use BigDecimal to avoid floating-point errors.
 */
public final class CurrencyUtils {

    private CurrencyUtils() {}

    public static final String DEFAULT_CURRENCY = "USD";
    public static final int    SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    /**
     * Rounds a monetary amount to standard banking precision (2 decimal places, HALF_EVEN).
     */
    public static BigDecimal round(BigDecimal amount) {
        if (amount == null) throw new IllegalArgumentException("Amount cannot be null");
        return amount.setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Adds two monetary amounts safely.
     */
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return round(a.add(b));
    }

    /**
     * Subtracts b from a.
     */
    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return round(a.subtract(b));
    }

    /**
     * Formats an amount for display (e.g., "$1,234.56").
     */
    public static String format(BigDecimal amount, String currencyCode) {
        Currency currency = Currency.getInstance(currencyCode);
        return String.format("%s %,.2f", currency.getSymbol(Locale.US), amount);
    }

    /**
     * Formats in default USD.
     */
    public static String formatUSD(BigDecimal amount) {
        return format(amount, DEFAULT_CURRENCY);
    }

    /**
     * Returns true if the amount represents a debit (negative value).
     */
    public static boolean isDebit(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Converts cents (integer) to a BigDecimal dollar amount.
     */
    public static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE);
    }

    /**
     * Converts a dollar BigDecimal to cents (long).
     */
    public static long toCents(BigDecimal dollars) {
        return round(dollars).multiply(BigDecimal.valueOf(100)).longValueExact();
    }
}
