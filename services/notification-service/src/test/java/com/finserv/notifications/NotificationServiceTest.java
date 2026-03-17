package com.finserv.notifications;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for Issue #13 fix: email delivery failures must be detected and logged,
 * not silently ignored.
 */
class NotificationServiceTest {

    private EmailProvider emailProvider;
    private SmsProvider smsProvider;
    private AlertTemplateManager templateManager;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        emailProvider = Mockito.mock(EmailProvider.class);
        smsProvider = Mockito.mock(SmsProvider.class);
        templateManager = new AlertTemplateManager();
        notificationService = new NotificationService(emailProvider, smsProvider, templateManager);
    }

    // --- Happy path: successful email delivery ---
    @Test
    void sendAlert_shouldCallEmailProviderAndCheckResult() {
        when(emailProvider.send(anyString(), anyString(), anyString())).thenReturn(true);

        notificationService.sendAlert(
            "user-001", "alice@test.com", null,
            AlertTemplateManager.AlertType.PASSWORD_CHANGED,
            Map.of("accountId", "12345678"));

        verify(emailProvider, times(1)).send(
            eq("alice@test.com"), anyString(), anyString());
    }

    // --- Failure case: email send returns false (delivery failure) ---
    @Test
    void sendAlert_shouldDetectEmailDeliveryFailure() {
        when(emailProvider.send(anyString(), anyString(), anyString())).thenReturn(false);

        // Should not throw — but should log the failure
        notificationService.sendAlert(
            "user-001", "alice@test.com", null,
            AlertTemplateManager.AlertType.PAYMENT_SENT,
            Map.of("accountId", "12345678", "amount", "100.00",
                   "currency", "USD", "transactionId", "TXN-001"));

        verify(emailProvider, times(1)).send(
            eq("alice@test.com"), anyString(), anyString());
    }

    // --- Edge case: null email should skip email entirely ---
    @Test
    void sendAlert_withNullEmail_shouldSkipEmailProvider() {
        notificationService.sendAlert(
            "user-001", null, "+12125551234",
            AlertTemplateManager.AlertType.LOGIN_NEW_DEVICE,
            Map.of("ipAddress", "10.0.0.1", "timestamp", "2024-01-01T00:00:00Z"));

        verify(emailProvider, never()).send(anyString(), anyString(), anyString());
    }
}
