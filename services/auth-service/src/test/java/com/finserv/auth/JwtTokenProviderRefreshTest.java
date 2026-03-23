package com.finserv.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtTokenProvider.refreshAccessToken() — fixes Issue #2.
 */
class JwtTokenProviderRefreshTest {

    private JwtTokenProvider provider;
    private static final String SECRET = Base64.getEncoder().encodeToString(
        "test-secret-key-that-is-long-enough-for-hs256!!".getBytes());

    @BeforeEach
    void setUp() throws Exception {
        provider = new JwtTokenProvider();
        Field secretField = JwtTokenProvider.class.getDeclaredField("jwtSecret");
        secretField.setAccessible(true);
        secretField.set(provider, SECRET);
    }

    // Happy path: valid refresh token produces a new access token
    @Test
    void validRefreshToken_returnsNewAccessToken() {
        String refreshToken = provider.generateRefreshToken("user-001");
        String newAccessToken = provider.refreshAccessToken(refreshToken);

        assertNotNull(newAccessToken);
        Claims claims = provider.validateToken(newAccessToken);
        assertEquals("user-001", claims.getSubject());
        assertEquals("access", claims.get("type", String.class));
    }

    // Rejection case: access token submitted as refresh token must be rejected
    @Test
    void accessTokenUsedAsRefresh_throwsException() {
        String accessToken = provider.generateAccessToken("user-001", "USER");

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> provider.refreshAccessToken(accessToken)
        );
        assertEquals("Token is not a refresh token", ex.getMessage());
    }

    // Edge case: token with no type claim must be rejected
    @Test
    void tokenWithNoTypeClaim_throwsException() {
        String tokenNoType = buildTokenWithClaims(Map.of("role", "USER"), "user-001");

        assertThrows(
            IllegalArgumentException.class,
            () -> provider.refreshAccessToken(tokenNoType)
        );
    }

    private String buildTokenWithClaims(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 60_000L);
        return Jwts.builder()
                   .setClaims(new HashMap<>(claims))
                   .setSubject(subject)
                   .setIssuedAt(now)
                   .setExpiration(expiry)
                   .signWith(SignatureAlgorithm.HS256,
                             Base64.getDecoder().decode(SECRET))
                   .compact();
    }
}
