package com.finserv.notifications;

import com.finserv.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core notification orchestration service.
 * Dispatches email and/or SMS alerts based on user notification preferences.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final EmailProvider        emailProvider;
    private final SmsProvider          smsProvider;
    private final AlertTemplateManager templateManager;

    private final Set<String> recentlySent = ConcurrentHashMap.newKeySet();

    public NotificationService(EmailProvider emailProvider,
                                SmsProvider smsProvider,
                                AlertTemplateManager templateManager) {
        this.emailProvider   = emailProvider;
        this.smsProvider     = smsProvider;
        this.templateManager = templateManager;
    }

    public void sendAlert(String userId, String email, String phoneNumber,
                          AlertTemplateManager.AlertType alertType,
                          Map<String, Object> context) {

        String eventId = context.getOrDefault("transactionId", context.getOrDefault("eventId", "")).toString();
        String dedupKey = userId + ":" + alertType.name() + ":" + eventId;

        if (recentlySent.contains(dedupKey)) {
            log.debug("Suppressing duplicate notification for key: {}", dedupKey);
            return;
        }
        recentlySent.add(dedupKey);

        String subject = templateManager.buildSubject(alertType, (String) context.get("accountId"));
        String body    = templateManager.buildBody(alertType, context);

        if (email != null && ValidationUtils.isValidEmail(email)) {
            // BUG (Issue #13): return value not checked — silent failure
            emailProvider.send(email, subject, body);
        }

        if (phoneNumber != null && ValidationUtils.isValidPhoneNumber(phoneNumber)) {
            String smsBody = body.length() > 160 ? body.substring(0, 157) + "..." : body;
            smsProvider.send(phoneNumber, smsBody);
        }

        log.info("Alert sent: userId={}, type={}", userId, alertType);
    }

    /**
     * Convenience method for payment alerts.
     */
    public void sendPaymentAlert(String userId, String email, String phone,
                                  String accountId, String amount, String currency,
                                  String transactionId, boolean isSender) {

        AlertTemplateManager.AlertType type = isSender
            ? AlertTemplateManager.AlertType.PAYMENT_SENT
            : AlertTemplateManager.AlertType.PAYMENT_RECEIVED;

        sendAlert(userId, email, phone, type, Map.of(
            "accountId",     accountId,
            "amount",        amount,
            "currency",      currency,
            "transactionId", transactionId
        ));
    }
}
