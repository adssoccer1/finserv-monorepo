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
import java.util.regex.Pattern;

/**
 * Logs all inbound API requests and outbound responses for audit and debugging.
 * Sensitive fields (card numbers, account numbers, passwords) are masked before logging
 * per PCI-DSS Requirement 3.3.
 */
@Component
@Order(1)
public class LoggingMiddleware extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingMiddleware.class);

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

    private String getRequestId(HttpServletRequest request) {
        String id = request.getHeader("X-Request-Id");
        return id != null ? id : "no-req-id";
    }

    // Patterns to match sensitive JSON fields and mask their values
    private static final Pattern CARD_NUMBER_PATTERN =
        Pattern.compile("(\"cardNumber\"\\s*:\\s*\")([^\"]+)(\")", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACCOUNT_PATTERN =
        Pattern.compile("(\"(?:accountNumber|sourceAccountId|destinationAccountId)\"\\s*:\\s*\")([^\"]+)(\")", Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSWORD_PATTERN =
        Pattern.compile("(\"(?:password|rawPassword|currentPassword|newPassword)\"\\s*:\\s*\")([^\"]+)(\")", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN_PATTERN =
        Pattern.compile("(\"(?:accessToken|refreshToken|token)\"\\s*:\\s*\")([^\"]+)(\")", Pattern.CASE_INSENSITIVE);

    static String maskSensitiveFields(String body) {
        if (body == null || body.isBlank()) return body;
        String masked = CARD_NUMBER_PATTERN.matcher(body).replaceAll("$1****-****-****-XXXX$3");
        masked = ACCOUNT_PATTERN.matcher(masked).replaceAll(mr -> {
            String val = mr.group(2);
            String suffix = val.length() >= 4 ? val.substring(val.length() - 4) : val;
            return mr.group(1) + "***" + suffix + mr.group(3);
        });
        masked = PASSWORD_PATTERN.matcher(masked).replaceAll("$1[REDACTED]$3");
        masked = TOKEN_PATTERN.matcher(masked).replaceAll("$1[REDACTED]$3");
        return masked;
    }
}
