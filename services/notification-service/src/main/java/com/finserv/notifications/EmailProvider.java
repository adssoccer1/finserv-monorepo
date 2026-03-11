package com.finserv.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Low-level email delivery via JavaMailSender (backed by SendGrid SMTP in prod).
 *
 * BUG (Issue #13 - medium): send() catches MailException and returns false,
 * but callers (NotificationService) do not check the return value.
 * Failed email deliveries are silently swallowed with only a WARN log.
 * There is no retry, no dead-letter queue, and no alerting.
 *
 * Discovered after a customer complained they never received their
 * 2FA code. Root cause: SMTP relay was rate-limiting, all sends failed
 * silently for ~40 minutes.
 */
@Component
public class EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(EmailProvider.class);

    private final JavaMailSender mailSender;

    @Value("${finserv.email.from:noreply@meridianbank.com}")
    private String fromAddress;

    @Value("${finserv.email.enabled:true}")
    private boolean enabled;

    public EmailProvider(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a plain-text email. Returns true on success, false on failure.
     *
     * BUG: Callers do not check the return value — failures are silently ignored.
     */
    public boolean send(String to, String subject, String body) {
        if (!enabled) {
            log.debug("Email disabled — skipping send to {}", to);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {} | subject: {}", to, subject);
            return true;
        } catch (MailException e) {
            // BUG: exception swallowed — caller has no way to know this failed
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
            return false;  // caller ignores this
        }
    }

    /**
     * Sends an HTML email (used for transaction receipts).
     */
    public boolean sendHtml(String to, String subject, String htmlBody) {
        // Simplified: delegates to plain text for now
        // TODO: use MimeMessageHelper for real HTML support (FIN-2567)
        String plainText = htmlBody.replaceAll("<[^>]+>", "");
        return send(to, subject, plainText);
    }
}
