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

@Component
@Order(1)
public class LoggingMiddleware extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingMiddleware.class);

    private static final Pattern[] SENSITIVE_PATTERNS = {
        Pattern.compile("(\"cardNumber\"\\s*:\\s*\")[^\"]*(\")"),
        Pattern.compile("(\"accountNumber\"\\s*:\\s*\")[^\"]*(\")"),
        Pattern.compile("(\"password\"\\s*:\\s*\")[^\"]*(\")"),
        Pattern.compile("(\"newPassword\"\\s*:\\s*\")[^\"]*(\")"),
        Pattern.compile("(\"currentPassword\"\\s*:\\s*\")[^\"]*(\")"),
        Pattern.compile("(\"token\"\\s*:\\s*\")[^\"]*(\")"),
        Pattern.compile("(\"refreshToken\"\\s*:\\s*\")[^\"]*(\")"),
        Pattern.compile("(\"accessToken\"\\s*:\\s*\")[^\"]*(\")"),
        Pattern.compile("(\"ssn\"\\s*:\\s*\")[^\"]*(\")"),
        Pattern.compile("(\"routingNumber\"\\s*:\\s*\")[^\"]*(\")"),
    };

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
        if (body == null || body.isEmpty()) return body;
        String masked = body;
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            masked = pattern.matcher(masked).replaceAll("$1***$2");
        }
        return masked;
    }

    private String getRequestId(HttpServletRequest request) {
        String id = request.getHeader("X-Request-Id");
        return id != null ? id : "no-req-id";
    }
}
