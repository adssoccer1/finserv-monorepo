package com.finserv.notifications;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private EmailProvider emailProvider;
    @Mock private SmsProvider smsProvider;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        AlertTemplateManager templateManager = new AlertTemplateManager();
        service = new NotificationService(emailProvider, smsProvider, templateManager);
    }

    // --- Happy path: first alert is sent ---
    @Test
    void sendAlert_firstTime_sendsEmail() {
        when(emailProvider.send(anyString(), anyString(), anyString())).thenReturn(true);

        service.sendAlert("user-001", "alice@test.com", null,
                AlertTemplateManager.AlertType.PAYMENT_SENT,
                Map.of("accountId", "12345678", "amount", "100.00",
                       "currency", "USD", "transactionId", "TXN-001"));

        verify(emailProvider).send(eq("alice@test.com"), anyString(), anyString());
    }

    // --- Rejection: duplicate with same event ID is suppressed ---
    @Test
    void sendAlert_duplicateEventId_suppressesSecondAlert() {
        when(emailProvider.send(anyString(), anyString(), anyString())).thenReturn(true);

        Map<String, Object> context = Map.of(
                "accountId", "12345678", "amount", "100.00",
                "currency", "USD", "transactionId", "TXN-002");

        service.sendAlert("user-001", "alice@test.com", null,
                AlertTemplateManager.AlertType.PAYMENT_SENT, context);
        service.sendAlert("user-001", "alice@test.com", null,
                AlertTemplateManager.AlertType.PAYMENT_SENT, context);

        // Email should only be sent once despite two calls
        verify(emailProvider, times(1)).send(eq("alice@test.com"), anyString(), anyString());
    }

    // --- Edge case: different event IDs are NOT deduplicated ---
    @Test
    void sendAlert_differentEventIds_sendsEachAlert() {
        when(emailProvider.send(anyString(), anyString(), anyString())).thenReturn(true);

        service.sendAlert("user-001", "alice@test.com", null,
                AlertTemplateManager.AlertType.PAYMENT_SENT,
                Map.of("accountId", "12345678", "amount", "100.00",
                       "currency", "USD", "transactionId", "TXN-003"));
        service.sendAlert("user-001", "alice@test.com", null,
                AlertTemplateManager.AlertType.PAYMENT_SENT,
                Map.of("accountId", "12345678", "amount", "200.00",
                       "currency", "USD", "transactionId", "TXN-004"));

        verify(emailProvider, times(2)).send(eq("alice@test.com"), anyString(), anyString());
    }
}
