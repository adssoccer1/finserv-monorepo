package com.finserv.notifications;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService deduplication — fixes Issue #4.
 */
class NotificationServiceDedupTest {

    private EmailProvider emailProvider;
    private SmsProvider smsProvider;
    private AlertTemplateManager templateManager;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        emailProvider = mock(EmailProvider.class);
        smsProvider = mock(SmsProvider.class);
        templateManager = new AlertTemplateManager();
        service = new NotificationService(emailProvider, smsProvider, templateManager);
        when(emailProvider.send(anyString(), anyString(), anyString())).thenReturn(true);
    }

    // Happy path: first notification with a given event ID is sent
    @Test
    void firstNotification_isSent() {
        service.sendAlert("user-001", "a@test.com", null,
            AlertTemplateManager.AlertType.PAYMENT_SENT,
            Map.of("accountId", "12345678", "amount", "100.00",
                   "currency", "USD", "transactionId", "TXN-001"));

        verify(emailProvider, times(1)).send(anyString(), anyString(), anyString());
    }

    // Rejection case: duplicate notification with same event ID is suppressed
    @Test
    void duplicateNotification_isSuppressed() {
        Map<String, Object> context = Map.of(
            "accountId", "12345678", "amount", "100.00",
            "currency", "USD", "transactionId", "TXN-002");

        service.sendAlert("user-001", "a@test.com", null,
            AlertTemplateManager.AlertType.PAYMENT_SENT, context);
        service.sendAlert("user-001", "a@test.com", null,
            AlertTemplateManager.AlertType.PAYMENT_SENT, context);

        // Email should only be sent once despite two calls
        verify(emailProvider, times(1)).send(anyString(), anyString(), anyString());
    }

    // Edge case: different transaction IDs should NOT be deduplicated
    @Test
    void differentTransactionIds_bothSent() {
        service.sendAlert("user-001", "a@test.com", null,
            AlertTemplateManager.AlertType.PAYMENT_SENT,
            Map.of("accountId", "12345678", "amount", "100.00",
                   "currency", "USD", "transactionId", "TXN-003"));
        service.sendAlert("user-001", "a@test.com", null,
            AlertTemplateManager.AlertType.PAYMENT_SENT,
            Map.of("accountId", "12345678", "amount", "200.00",
                   "currency", "USD", "transactionId", "TXN-004"));

        verify(emailProvider, times(2)).send(anyString(), anyString(), anyString());
    }
}
