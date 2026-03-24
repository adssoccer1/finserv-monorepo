package com.finserv.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Logs all inbound API requests and outbound responses for audit and debugging.
 * Sensitive fields (card numbers, account numbers, passwords, tokens) are masked
 * before logging to comply with PCI-DSS requirements.
 */
@Component
@Order(1)
public class LoggingMiddleware extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingMiddleware.class);

    private static final Set<String> SENSITIVE_FIELD_NAMES = Set.of(
        "cardNumber", "card_number", "cardnumber",
        "accountNumber", "account_number", "accountnumber",
        "password", "rawPassword", "newPassword", "currentPassword",
        "token", "accessToken", "refreshToken", "access_token", "refresh_token",
        "ssn", "socialSecurityNumber",
        "routingNumber", "routing_number",
        "cvv", "cvc", "securityCode"
    );

    private static final Pattern JSON_FIELD_PATTERN = Pattern.compile(
        "\"(" + String.join("|", SENSITIVE_FIELD_NAMES) + ")\"\\s*:\\s*\"([^\"]*)\""
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest   = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            String requestBody = new String(
                wrappedRequest.getContentAsByteArray(), StandardCharsets.UTF_8);

            String maskedBody = maskSensitiveFields(requestBody);

            log.info("[{}] {} {} | body={} | status={} | {}ms",
                     getRequestId(request),
                     request.getMethod(),
                     request.getRequestURI(),
                     maskedBody,
                     wrappedResponse.getStatus(),
                     duration);

            wrappedResponse.copyBodyToResponse();
        }
    }

    static String maskSensitiveFields(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }
        return JSON_FIELD_PATTERN.matcher(body).replaceAll(mr -> {
            String fieldName = mr.group(1);
            String value = mr.group(2);
            String masked = maskValue(value);
            return "\"" + fieldName + "\":\"" + masked + "\"";
        });
    }

    private static String maskValue(String value) {
        if (value == null || value.length() <= 4) {
            return "***";
        }
        return "***" + value.substring(value.length() - 4);
    }

    private String getRequestId(HttpServletRequest request) {
        String id = request.getHeader("X-Request-Id");
        return id != null ? id : "no-req-id";
    }
}
