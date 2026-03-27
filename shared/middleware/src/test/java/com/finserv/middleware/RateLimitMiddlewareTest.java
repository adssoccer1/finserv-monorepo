package com.finserv.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

class RateLimitMiddlewareTest {

    private RateLimitMiddleware filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitMiddleware();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    void allowsRequestsUpToLimit() throws Exception {
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        for (int i = 0; i < 100; i++) {
            filter.doFilterInternal(request, response, chain);
        }

        verify(chain, times(100)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void blocksRequestAtExactLimit() throws Exception {
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        // Send 101 requests — the 101st should be blocked
        for (int i = 0; i < 101; i++) {
            filter.doFilterInternal(request, response, chain);
        }

        verify(chain, times(100)).doFilter(request, response);
        verify(response, times(1)).setStatus(429);
    }

    @Test
    void differentIpsHaveSeparateLimits() throws Exception {
        HttpServletRequest request2 = mock(HttpServletRequest.class);
        when(request2.getRemoteAddr()).thenReturn("192.168.1.1");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        // Exhaust limit for IP 1
        for (int i = 0; i < 100; i++) {
            filter.doFilterInternal(request, response, chain);
        }

        // IP 2 should still be allowed
        filter.doFilterInternal(request2, response, chain);

        verify(chain, times(101)).doFilter(any(), eq(response));
    }
}
