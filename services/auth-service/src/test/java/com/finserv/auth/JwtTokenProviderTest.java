package com.finserv.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;
    private static final String SECRET = Base64.getEncoder().encodeToString(
            "test-secret-key-that-is-at-least-32-bytes-long!!".getBytes());

    @BeforeEach
    void setUp() throws Exception {
        provider = new JwtTokenProvider();
        Field secretField = JwtTokenProvider.class.getDeclaredField("jwtSecret");
        secretField.setAccessible(true);
        secretField.set(provider, SECRET);
    }

    // === Issue #2: refreshAccessToken must reject access tokens ===

    @Test
    @DisplayName("#2 happy path: refresh token is accepted for refresh")
    void refreshAccessToken_withRefreshToken_returnsNewAccessToken() {
        String refreshToken = provider.generateRefreshToken("user-001");
        String newAccessToken = provider.refreshAccessToken(refreshToken);
        assertNotNull(newAccessToken);

        Claims claims = provider.validateToken(newAccessToken);
        assertEquals("user-001", claims.getSubject());
        assertEquals("access", claims.get("type"));
    }

    @Test
    @DisplayName("#2 rejection: access token is rejected for refresh")
    void refreshAccessToken_withAccessToken_throwsSecurityException() {
        String accessToken = provider.generateAccessToken("user-001", "USER");
        assertThrows(SecurityException.class, () -> provider.refreshAccessToken(accessToken));
    }

    @Test
    @DisplayName("#2 edge: token without type claim is rejected")
    void refreshAccessToken_withNoTypeClaim_throwsSecurityException() {
        // Build a token with no 'type' claim
        String token = Jwts.builder()
                .setSubject("user-001")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(SignatureAlgorithm.HS256, Base64.getDecoder().decode(SECRET))
                .compact();
        assertThrows(SecurityException.class, () -> provider.refreshAccessToken(token));
    }
}
