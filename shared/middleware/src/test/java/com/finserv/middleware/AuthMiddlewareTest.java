package com.finserv.middleware;

import com.finserv.utils.ErrorCodes;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for AuthMiddleware JWT algorithm enforcement (Issue #12).
 */
class AuthMiddlewareTest {

    private AuthMiddleware middleware;
    private String base64Secret;

    @BeforeEach
    void setUp() throws Exception {
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        base64Secret = Base64.getEncoder().encodeToString(key.getEncoded());

        middleware = new AuthMiddleware();
        Field secretField = AuthMiddleware.class.getDeclaredField("jwtSecret");
        secretField.setAccessible(true);
        secretField.set(middleware, base64Secret);
    }

    @Test
    void validHs256Token_isAccepted() throws Exception {
        String token = Jwts.builder()
                .setSubject("user-1")
                .claim("role", "USER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(SignatureAlgorithm.HS256, Base64.getDecoder().decode(base64Secret))
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/payments");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        middleware.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertNotNull(request.getAttribute("userId"));
        assertEquals("user-1", request.getAttribute("userId"));
    }

    @Test
    void algNoneToken_isRejected() throws Exception {
        // Manually craft an unsigned token with alg=none.
        // Header: {"alg":"none"}  Payload: {"sub":"attacker","role":"ADMIN","exp":<future>}
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes());
        long exp = (System.currentTimeMillis() / 1000) + 3600;
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"sub\":\"attacker\",\"role\":\"ADMIN\",\"exp\":" + exp + "}").getBytes());
        String algNoneToken = header + "." + payload + ".";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/payments");
        request.addHeader("Authorization", "Bearer " + algNoneToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        middleware.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
    }

    @Test
    void missingAuthHeader_isRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/payments");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        middleware.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
    }

    @Test
    void publicPath_isAllowedWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        middleware.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }
}
