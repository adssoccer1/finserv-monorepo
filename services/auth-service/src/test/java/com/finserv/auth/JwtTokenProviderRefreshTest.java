package com.finserv.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JwtTokenProvider.refreshAccessToken() — verifies that
 * access tokens are rejected when used as refresh tokens (fix for Issue #2).
 */
class JwtTokenProviderRefreshTest {

    private JwtTokenProvider tokenProvider;
    private static final String TEST_SECRET =
        Base64.getEncoder().encodeToString(
            "this-is-a-test-secret-key-for-hs256-algorithm!!".getBytes());

    @BeforeEach
    void setUp() throws Exception {
        tokenProvider = new JwtTokenProvider();
        Field secretField = JwtTokenProvider.class.getDeclaredField("jwtSecret");
        secretField.setAccessible(true);
        secretField.set(tokenProvider, TEST_SECRET);
    }

    // Happy path: refresh token is accepted
    @Test
    void refreshAccessToken_withValidRefreshToken_succeeds() {
        String refreshToken = tokenProvider.generateRefreshToken("user-001");
        String newAccessToken = tokenProvider.refreshAccessToken(refreshToken);
        assertNotNull(newAccessToken);

        Claims claims = tokenProvider.validateToken(newAccessToken);
        assertEquals("user-001", claims.getSubject());
        assertEquals("access", claims.get("type", String.class));
    }

    // Rejection: access token used as refresh token must fail
    @Test
    void refreshAccessToken_withAccessToken_throwsSecurityException() {
        String accessToken = tokenProvider.generateAccessToken("user-001", "USER");
        assertThrows(SecurityException.class, () ->
            tokenProvider.refreshAccessToken(accessToken));
    }

    // Edge case: token with no type claim must fail
    @Test
    void refreshAccessToken_withNoTypeClaim_throwsSecurityException() {
        Key key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");
        // No "type" claim
        String tokenNoType = Jwts.builder()
            .setClaims(claims)
            .setSubject("user-001")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 60000))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();

        assertThrows(SecurityException.class, () ->
            tokenProvider.refreshAccessToken(tokenNoType));
    }
}
