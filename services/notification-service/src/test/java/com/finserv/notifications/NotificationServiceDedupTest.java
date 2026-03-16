package com.finserv.notifications;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for NotificationService deduplication — verifies that the dedup key
 * uses the event ID instead of a timestamp bucket (fix for Issue #4).
 */
class NotificationServiceDedupTest {

    private NotificationService service;
    private EmailProvider emailProvider;
    private SmsProvider smsProvider;
    private AlertTemplateManager templateManager;

    @BeforeEach
    void setUp() {
        emailProvider = mock(EmailProvider.class);
        smsProvider = mock(SmsProvider.class);
        templateManager = mock(AlertTemplateManager.class);
        service = new NotificationService(emailProvider, smsProvider, templateManager);

        when(templateManager.buildSubject(any(), anyString())).thenReturn("Test Subject");
        when(templateManager.buildBody(any(), any())).thenReturn("Test Body");
        when(emailProvider.send(anyString(), anyString(), anyString())).thenReturn(true);
    }

    // Happy path: first alert with eventId is sent
    @Test
    void sendAlert_firstCall_sendsNotification() {
        service.sendAlert("user-001", "test@example.com", null,
            AlertTemplateManager.AlertType.PAYMENT_SENT,
            Map.of("accountId", "123", "eventId", "PAY-001"));

        verify(emailProvider, times(1)).send(anyString(), anyString(), anyString());
    }

    // Rejection: duplicate eventId is suppressed
    @Test
    void sendAlert_duplicateEventId_suppressesSecondCall() {
        Map<String, Object> ctx = Map.of("accountId", "123", "eventId", "PAY-001");
        service.sendAlert("user-001", "test@example.com", null,
            AlertTemplateManager.AlertType.PAYMENT_SENT, ctx);
        service.sendAlert("user-001", "test@example.com", null,
            AlertTemplateManager.AlertType.PAYMENT_SENT, ctx);

        verify(emailProvider, times(1)).send(anyString(), anyString(), anyString());
    }

    // Edge case: different eventIds are NOT suppressed
    @Test
    void sendAlert_differentEventIds_sendsAll() {
        service.sendAlert("user-001", "test@example.com", null,
            AlertTemplateManager.AlertType.PAYMENT_SENT,
            Map.of("accountId", "123", "eventId", "PAY-001"));
        service.sendAlert("user-001", "test@example.com", null,
            AlertTemplateManager.AlertType.PAYMENT_SENT,
            Map.of("accountId", "123", "eventId", "PAY-002"));

        verify(emailProvider, times(2)).send(anyString(), anyString(), anyString());
    }
}
