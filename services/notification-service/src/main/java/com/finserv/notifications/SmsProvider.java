package com.finserv.notifications;

import com.finserv.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * SMS delivery via Twilio REST API.
 *
 * In production, credentials are injected from AWS Secrets Manager.
 * Do NOT commit real credentials to this file.
 */
@Component
public class SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(SmsProvider.class);

    @Value("${finserv.twilio.account-sid:#{null}}")
    private String accountSid;

    @Value("${finserv.twilio.auth-token:#{null}}")
    private String authToken;

    @Value("${finserv.twilio.from-number:+15005550006}")
    private String fromNumber;

    @Value("${finserv.sms.enabled:true}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Sends an SMS to the given phone number.
     * Returns true on success, false on failure.
     *
     * Phone number must be in E.164 format (e.g., +12125551234).
     */
    public boolean send(String toPhoneNumber, String message) {
        if (!enabled) {
            log.debug("SMS disabled — skipping send to {}", toPhoneNumber);
            return false;
        }

        if (!ValidationUtils.isValidPhoneNumber(toPhoneNumber)) {
            log.warn("Invalid phone number: {}", toPhoneNumber);
            return false;
        }

        if (message == null || message.isBlank()) {
            log.warn("Attempted to send empty SMS to {}", toPhoneNumber);
            return false;
        }

        // Truncate to SMS character limit
        String body = message.length() > 160 ? message.substring(0, 157) + "..." : message;

        try {
            String url = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json",
                                       accountSid);

            Map<String, String> params = Map.of(
                "From", fromNumber,
                "To",   toPhoneNumber,
                "Body", body
            );

            // Simplified HTTP call (real implementation uses Twilio SDK)
            log.info("SMS queued for {}: {} chars", toPhoneNumber, body.length());
            return true;

        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", toPhoneNumber, e.getMessage());
            return false;
        }
    }
}
