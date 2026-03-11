package com.finserv.utils;

/**
 * Canonical error codes used across all FinServ services.
 * Last updated: 2022-08-14 (original), various additions since.
 */
public final class ErrorCodes {

    private ErrorCodes() {}

    // Authentication errors
    public static final String AUTH_TOKEN_EXPIRED       = "AUTH_001";
    public static final String AUTH_TOKEN_INVALID       = "AUTH_002";
    public static final String AUTH_CREDENTIALS_INVALID = "AUTH_003";
    public static final String AUTH_SESSION_NOT_FOUND   = "AUTH_004";
    public static final String AUTH_ACCOUNT_LOCKED      = "AUTH_005";

    // Payment errors
    public static final String PAYMENT_INSUFFICIENT_FUNDS   = "PAY_001";
    public static final String PAYMENT_INVALID_AMOUNT       = "PAY_002";
    public static final String PAYMENT_ACCOUNT_NOT_FOUND    = "PAY_003";
    public static final String PAYMENT_DUPLICATE_REQUEST    = "PAY_004";
    public static final String PAYMENT_RATE_LIMIT_EXCEEDED  = "PAY_005";
    public static final String PAYMENT_PROCESSING_ERROR     = "PAY_006";

    // Transaction errors
    public static final String TXN_NOT_FOUND            = "TXN_001";
    public static final String TXN_ALREADY_RECONCILED   = "TXN_002";
    public static final String TXN_LEDGER_IMBALANCE     = "TXN_003";

    // Notification errors
    public static final String NOTIF_DELIVERY_FAILED    = "NOTIF_001";
    public static final String NOTIF_TEMPLATE_NOT_FOUND = "NOTIF_002";
    public static final String NOTIF_INVALID_RECIPIENT  = "NOTIF_003";

    // Generic
    public static final String INTERNAL_ERROR           = "SYS_001";
    public static final String VALIDATION_ERROR         = "SYS_002";
    public static final String RESOURCE_NOT_FOUND       = "SYS_003";
}
