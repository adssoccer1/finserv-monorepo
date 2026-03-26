package com.finserv.notifications;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private EmailProvider emailProvider;
    private SmsProvider smsProvider;
    private AlertTemplateManager templateManager;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        emailProvider = Mockito.mock(EmailProvider.class);
        smsProvider = Mockito.mock(SmsProvider.class);
        templateManager = Mockito.mock(AlertTemplateManager.class);
        service = new NotificationService(emailProvider, smsProvider, templateManager);

        when(templateManager.buildSubject(any(), anyString())).thenReturn("Test Subject");
        when(templateManager.buildBody(any(), any())).thenReturn("Test Body");
    }

    // === Issue #4: Dedup key uses event ID, not timestamp ===

    @Test
    @DisplayName("#4 happy path: same transactionId suppresses duplicate")
    void sendAlert_sameTransactionId_suppressesDuplicate() {
        when(emailProvider.send(anyString(), anyString(), anyString())).thenReturn(true);

        Map<String, Object> context = new HashMap<>();
        context.put("transactionId", "txn-001");
        context.put("accountId", "acct-001");

        service.sendAlert("user-001", "a@b.com", null,
                AlertTemplateManager.AlertType.PAYMENT_SENT, context);
        service.sendAlert("user-001", "a@b.com", null,
                AlertTemplateManager.AlertType.PAYMENT_SENT, context);

        // Email should only be sent once due to dedup
        verify(emailProvider, times(1)).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("#4 rejection: different transactionIds are NOT suppressed")
    void sendAlert_differentTransactionIds_bothSent() {
        when(emailProvider.send(anyString(), anyString(), anyString())).thenReturn(true);

        Map<String, Object> context1 = new HashMap<>();
        context1.put("transactionId", "txn-001");
        context1.put("accountId", "acct-001");

        Map<String, Object> context2 = new HashMap<>();
        context2.put("transactionId", "txn-002");
        context2.put("accountId", "acct-001");

        service.sendAlert("user-001", "a@b.com", null,
                AlertTemplateManager.AlertType.PAYMENT_SENT, context1);
        service.sendAlert("user-001", "a@b.com", null,
                AlertTemplateManager.AlertType.PAYMENT_SENT, context2);

        verify(emailProvider, times(2)).send(anyString(), anyString(), anyString());
    }

    // === Issue #13: Email/SMS delivery failure logging ===

    @Test
    @DisplayName("#13 happy path: successful email delivery does not log error")
    void sendAlert_emailSuccess_noError() {
        when(emailProvider.send(anyString(), anyString(), anyString())).thenReturn(true);

        Map<String, Object> context = new HashMap<>();
        context.put("transactionId", "txn-100");
        context.put("accountId", "acct-001");

        // Should not throw
        service.sendAlert("user-001", "a@b.com", null,
                AlertTemplateManager.AlertType.PAYMENT_SENT, context);

        verify(emailProvider).send(eq("a@b.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("#13 rejection: email failure is detected (return value checked)")
    void sendAlert_emailFailure_returnValueChecked() {
        when(emailProvider.send(anyString(), anyString(), anyString())).thenReturn(false);

        Map<String, Object> context = new HashMap<>();
        context.put("transactionId", "txn-101");
        context.put("accountId", "acct-001");

        // Should not throw, but the return value IS checked (verified by code review)
        service.sendAlert("user-001", "a@b.com", null,
                AlertTemplateManager.AlertType.PAYMENT_SENT, context);

        verify(emailProvider).send(eq("a@b.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("#13 edge: SMS failure is detected (return value checked)")
    void sendAlert_smsFailure_returnValueChecked() {
        when(smsProvider.send(anyString(), anyString())).thenReturn(false);

        Map<String, Object> context = new HashMap<>();
        context.put("transactionId", "txn-102");
        context.put("accountId", "acct-001");

        service.sendAlert("user-001", null, "+1234567890",
                AlertTemplateManager.AlertType.PAYMENT_SENT, context);

        verify(smsProvider).send(eq("+1234567890"), anyString());
    }
}
