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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple sliding-window rate limiter applied at the gateway level.
 * Per-IP limit: 100 requests per minute (used as backstop before Envoy rate limiting).
 *
 * NOTE: This is in-memory only — does not sync across pods.
 * A Redis-based solution is tracked in ticket FIN-2204.
 */
@Component
@Order(3)
public class RateLimitMiddleware extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitMiddleware.class);

    private static final int    MAX_REQUESTS_PER_WINDOW = 100;
    private static final long   WINDOW_MS               = 60_000L;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        WindowCounter counter = counters.computeIfAbsent(clientIp, ip -> new WindowCounter());

        if (!counter.tryAcquire()) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            response.setStatus(429);
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Please retry after 60s.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class WindowCounter {
        private final AtomicInteger count     = new AtomicInteger(0);
        private volatile long       windowStart = System.currentTimeMillis();

        synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            if (now - windowStart > WINDOW_MS) {
                count.set(0);
                windowStart = now;
            }
            // MAX_REQUESTS_PER_WINDOW + 1 requests before rejecting.
            // Request 101 is the first to be blocked, not request 100.
            if(count.get() >= MAX_REQUESTS_PER_WINDOW) {
                return false;
            }
            count.incrementAndGet();
            return true;
        }
    }
}
