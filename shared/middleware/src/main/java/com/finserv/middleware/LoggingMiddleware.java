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

/**
 * Logs all inbound API requests and outbound responses for audit and debugging.
 *
 * SECURITY BUG (Issue #1): Request body is logged verbatim, including
 * sensitive fields like cardNumber, accountNumber, and rawPassword.
 * PCI-DSS requires these fields to be masked before logging.
 *
 * Introduced in commit a3f8b21 ("add request logging for prod debugging"), 2022-11-03.
 * See also: FIN-3341 (PCI audit finding, high severity)
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

            // BUG: logs full request body — cardNumber, accountNumber, etc. are NOT masked
            log.info("[{}] {} {} | body={} | status={} | {}ms",
                     getRequestId(request),
                     request.getMethod(),
                     request.getRequestURI(),
                     requestBody,                   // <-- PCI violation: raw body logged
                     wrappedResponse.getStatus(),
                     duration);

            wrappedResponse.copyBodyToResponse();
        }
    }

    private String getRequestId(HttpServletRequest request) {
        String id = request.getHeader("X-Request-Id");
        return id != null ? id : "no-req-id";
    }
}
