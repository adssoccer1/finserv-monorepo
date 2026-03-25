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

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private static final String TEST_SECRET = Base64.getEncoder().encodeToString(
        "test-secret-key-that-is-long-enough-for-HS256-algorithm!!".getBytes());

    @BeforeEach
    void setUp() throws Exception {
        tokenProvider = new JwtTokenProvider();
        Field secretField = JwtTokenProvider.class.getDeclaredField("jwtSecret");
        secretField.setAccessible(true);
        secretField.set(tokenProvider, TEST_SECRET);
    }

    // Happy path: refresh token is accepted and returns new access token
    @Test
    void refreshAccessToken_withValidRefreshToken_returnsNewAccessToken() {
        String refreshToken = tokenProvider.generateRefreshToken("user-001");
        String newAccessToken = tokenProvider.refreshAccessToken(refreshToken);
        assertNotNull(newAccessToken);

        Claims claims = tokenProvider.validateToken(newAccessToken);
        assertEquals("user-001", claims.getSubject());
        assertEquals("access", claims.get("type", String.class));
    }

    // Rejection: access token submitted as refresh token must be rejected
    @Test
    void refreshAccessToken_withAccessToken_throwsException() {
        String accessToken = tokenProvider.generateAccessToken("user-001", "USER");
        assertThrows(IllegalArgumentException.class,
            () -> tokenProvider.refreshAccessToken(accessToken));
    }

    // Edge case: token with no type claim must be rejected
    @Test
    void refreshAccessToken_withNoTypeClaim_throwsException() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");
        // Build a token without the "type" claim
        String tokenNoType = Jwts.builder()
            .setClaims(claims)
            .setSubject("user-001")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 60000))
            .signWith(SignatureAlgorithm.HS256,
                      Base64.getDecoder().decode(TEST_SECRET))
            .compact();

        assertThrows(IllegalArgumentException.class,
            () -> tokenProvider.refreshAccessToken(tokenNoType));
    }
}
