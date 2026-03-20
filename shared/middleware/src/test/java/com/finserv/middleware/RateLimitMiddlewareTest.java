package com.finserv.middleware;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitMiddlewareTest {

    private RateLimitMiddleware middleware;

    @BeforeEach
    void setUp() {
        middleware = new RateLimitMiddleware();
    }

    @Test
    void allowsRequestsUpToLimit() throws Exception {
        // First 100 requests should succeed (HTTP 200)
        for (int i = 1; i <= 100; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            middleware.doFilterInternal(request, response, chain);
            assertEquals(200, response.getStatus(),
                "Request #" + i + " should be allowed (status 200)");
        }
    }

    @Test
    void blocksRequestBeyondLimit() throws Exception {
        // Exhaust all 100 allowed requests
        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            middleware.doFilterInternal(request, response, chain);
        }

        // Request #101 should be blocked with 429
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        middleware.doFilterInternal(request, response, chain);

        assertEquals(429, response.getStatus(), "Request #101 should be rate-limited");
    }

    @Test
    void differentIpsHaveSeparateLimits() throws Exception {
        // Exhaust limit for IP A
        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            middleware.doFilterInternal(request, response, chain);
        }

        // IP B should still be allowed
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        middleware.doFilterInternal(request, response, chain);

        assertEquals(200, response.getStatus(), "Different IP should not be rate-limited");
    }
}
