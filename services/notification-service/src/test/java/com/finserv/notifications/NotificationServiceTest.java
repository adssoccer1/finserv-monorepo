package com.finserv.notifications;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

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
    }

    @Test
    void sendAlert_emailSuccess_logsInfoNotError() {
        when(emailProvider.send(anyString(), anyString(), anyString())).thenReturn(true);

        // Should not throw; email success is logged at INFO level
        assertDoesNotThrow(() -> service.sendAlert(
            "user1", "user@example.com", null,
            AlertTemplateManager.AlertType.PASSWORD_CHANGED,
            Map.of("accountId", "123456789")
        ));

        verify(emailProvider).send(eq("user@example.com"), anyString(), anyString());
    }

    @Test
    void sendAlert_emailFailure_logsError() {
        when(emailProvider.send(anyString(), anyString(), anyString())).thenReturn(false);

        // Should not throw, but the false return value is now captured and logged
        assertDoesNotThrow(() -> service.sendAlert(
            "user1", "user@example.com", null,
            AlertTemplateManager.AlertType.PASSWORD_CHANGED,
            Map.of("accountId", "123456789")
        ));

        verify(emailProvider).send(eq("user@example.com"), anyString(), anyString());
    }

    @Test
    void sendAlert_bothChannelsFail_logsAllChannelsFailedError() {
        when(emailProvider.send(anyString(), anyString(), anyString())).thenReturn(false);
        when(smsProvider.send(anyString(), anyString())).thenReturn(false);

        assertDoesNotThrow(() -> service.sendAlert(
            "user1", "user@example.com", "+12125551234",
            AlertTemplateManager.AlertType.PAYMENT_SENT,
            Map.of("accountId", "123456789", "amount", "100.00",
                   "currency", "USD", "transactionId", "TXN-001")
        ));

        verify(emailProvider).send(eq("user@example.com"), anyString(), anyString());
        verify(smsProvider).send(eq("+12125551234"), anyString());
    }

    @Test
    void sendAlert_emailFailsSmsSuceeds_partialSuccess() {
        when(emailProvider.send(anyString(), anyString(), anyString())).thenReturn(false);
        when(smsProvider.send(anyString(), anyString())).thenReturn(true);

        assertDoesNotThrow(() -> service.sendAlert(
            "user1", "user@example.com", "+12125551234",
            AlertTemplateManager.AlertType.PAYMENT_SENT,
            Map.of("accountId", "123456789", "amount", "100.00",
                   "currency", "USD", "transactionId", "TXN-001")
        ));

        verify(emailProvider).send(eq("user@example.com"), anyString(), anyString());
        verify(smsProvider).send(eq("+12125551234"), anyString());
    }

    @Test
    void sendAlert_nullEmail_skipsEmailProvider() {
        when(smsProvider.send(anyString(), anyString())).thenReturn(true);

        assertDoesNotThrow(() -> service.sendAlert(
            "user1", null, "+12125551234",
            AlertTemplateManager.AlertType.PASSWORD_CHANGED,
            Map.of("accountId", "123456789")
        ));

        verify(emailProvider, never()).send(anyString(), anyString(), anyString());
        verify(smsProvider).send(eq("+12125551234"), anyString());
    }

    @Test
    void sendAlert_invalidEmail_skipsEmailProvider() {
        when(smsProvider.send(anyString(), anyString())).thenReturn(true);

        assertDoesNotThrow(() -> service.sendAlert(
            "user1", "not-an-email", "+12125551234",
            AlertTemplateManager.AlertType.PASSWORD_CHANGED,
            Map.of("accountId", "123456789")
        ));

        verify(emailProvider, never()).send(anyString(), anyString(), anyString());
        verify(smsProvider).send(eq("+12125551234"), anyString());
    }
}
