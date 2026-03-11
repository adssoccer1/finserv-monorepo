package com.finserv.notifications;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Manages notification templates for account alerts.
 * Templates are stored as inline strings; production uses a Mustache template engine
 * with templates loaded from S3 (see FIN-1890).
 */
@Component
public class AlertTemplateManager {

    public enum AlertType {
        PAYMENT_SENT,
        PAYMENT_RECEIVED,
        LARGE_TRANSACTION,
        LOGIN_NEW_DEVICE,
        PASSWORD_CHANGED,
        ACCOUNT_LOCKED,
        LOW_BALANCE
    }

    public String buildSubject(AlertType alertType, String accountId) {
        return switch (alertType) {
            case PAYMENT_SENT       -> "Meridian Bank: Payment sent from account " + mask(accountId);
            case PAYMENT_RECEIVED   -> "Meridian Bank: Payment received to account " + mask(accountId);
            case LARGE_TRANSACTION  -> "Meridian Bank: Large transaction alert on " + mask(accountId);
            case LOGIN_NEW_DEVICE   -> "Meridian Bank: New device login detected";
            case PASSWORD_CHANGED   -> "Meridian Bank: Your password was changed";
            case ACCOUNT_LOCKED     -> "Meridian Bank: Your account has been locked";
            case LOW_BALANCE        -> "Meridian Bank: Low balance alert on " + mask(accountId);
        };
    }

    public String buildBody(AlertType alertType, Map<String, Object> context) {
        return switch (alertType) {
            case PAYMENT_SENT -> String.format(
                "A payment of %s %s was sent from your account ending in %s.\n" +
                "If you did not authorize this, contact us immediately at 1-800-MERIDIAN.\n\n" +
                "Transaction ID: %s",
                context.get("amount"), context.get("currency"),
                mask((String) context.get("accountId")),
                context.getOrDefault("transactionId", "N/A"));

            case PAYMENT_RECEIVED -> String.format(
                "A payment of %s %s was received into your account ending in %s.\n" +
                "Transaction ID: %s",
                context.get("amount"), context.get("currency"),
                mask((String) context.get("accountId")),
                context.getOrDefault("transactionId", "N/A"));

            case LOGIN_NEW_DEVICE -> String.format(
                "A login was detected from a new device or location.\n" +
                "IP: %s | Time: %s\n" +
                "If this was not you, change your password immediately.",
                context.getOrDefault("ipAddress", "unknown"),
                context.getOrDefault("timestamp", "unknown"));

            case PASSWORD_CHANGED ->
                "Your Meridian Bank password was successfully changed.\n" +
                "If you did not make this change, contact support immediately.";

            case ACCOUNT_LOCKED ->
                "Your account has been locked due to multiple failed login attempts.\n" +
                "Please contact support at 1-800-MERIDIAN to unlock your account.";

            case LOW_BALANCE -> String.format(
                "Your account ending in %s has a low balance of %s.\n" +
                "Log in to view your account at meridianbank.com.",
                mask((String) context.getOrDefault("accountId", "XXXXXX")),
                context.getOrDefault("balance", "$0.00"));

            default -> "Please log in to view your account activity at meridianbank.com.";
        };
    }

    private String mask(String accountId) {
        if (accountId == null || accountId.length() < 4) return "XXXX";
        return "..." + accountId.substring(accountId.length() - 4);
    }
}
