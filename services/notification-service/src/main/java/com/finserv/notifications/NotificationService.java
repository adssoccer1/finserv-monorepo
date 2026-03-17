package com.finserv.notifications;

import com.finserv.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core notification orchestration service.
 * Dispatches email and/or SMS alerts based on user notification preferences.
 *
 * BUG (Issue #4 - small): The deduplication key uses the current timestamp
 * (truncated to minute). On retry (e.g., after a transient email failure),
 * if the retry happens in the same minute, the dedup check prevents resending.
 * But if the retry happens a minute later, a duplicate IS sent because the
 * key has changed.
 *
 * A proper fix would use the event's own idempotency key (paymentId, etc.)
 * as the dedup key, independent of time.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final EmailProvider        emailProvider;
    private final SmsProvider          smsProvider;
    private final AlertTemplateManager templateManager;

    // BUG: dedup keys expire never (memory leak in long-running services)
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

        // BUG: dedup key uses timestamp-to-minute, not the event's own ID
        String dedupKey = userId + ":" + alertType.name() + ":"
                        + Instant.now().getEpochSecond() / 60;  // per-minute bucket

        if (recentlySent.contains(dedupKey)) {
            log.debug("Suppressing duplicate notification for key: {}", dedupKey);
            return;
        }
        recentlySent.add(dedupKey);

        String subject = templateManager.buildSubject(alertType, (String) context.get("accountId"));
        String body    = templateManager.buildBody(alertType, context);

        if (email != null && ValidationUtils.isValidEmail(email)) {
            boolean emailSent = emailProvider.send(email, subject, body);
            if (!emailSent) {
                log.warn("Email delivery failed for userId={}, alertType={}, recipient={}",
                         userId, alertType, email);
            }
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
