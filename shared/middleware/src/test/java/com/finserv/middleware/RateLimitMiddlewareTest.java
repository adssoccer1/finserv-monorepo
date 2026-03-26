package com.finserv.middleware;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitMiddlewareTest {

    // === Issue #8: Off-by-one in rate limiter ===

    @Test
    @DisplayName("#8 happy path: first request is allowed")
    void tryAcquire_firstRequest_returnsTrue() {
        RateLimitMiddleware.WindowCounter counter = new RateLimitMiddleware.WindowCounter();
        assertTrue(counter.tryAcquire());
    }

    @Test
    @DisplayName("#8 rejection: 101st request is blocked")
    void tryAcquire_101stRequest_returnsFalse() {
        RateLimitMiddleware.WindowCounter counter = new RateLimitMiddleware.WindowCounter();
        for (int i = 0; i < 100; i++) {
            assertTrue(counter.tryAcquire(), "Request " + (i + 1) + " should be allowed");
        }
        assertFalse(counter.tryAcquire(), "Request 101 should be blocked");
    }

    @Test
    @DisplayName("#8 edge: exactly 100 requests are allowed")
    void tryAcquire_exactly100Requests_allAllowed() {
        RateLimitMiddleware.WindowCounter counter = new RateLimitMiddleware.WindowCounter();
        int allowedCount = 0;
        for (int i = 0; i < 100; i++) {
            if (counter.tryAcquire()) allowedCount++;
        }
        assertEquals(100, allowedCount);
    }
}
