package com.finserv.auth;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private static final String TEST_SECRET = Base64.getEncoder().encodeToString(
            "super-secret-key-for-testing-only-32bytes!".getBytes());

    @BeforeEach
    void setUp() throws Exception {
        tokenProvider = new JwtTokenProvider();
        Field secretField = JwtTokenProvider.class.getDeclaredField("jwtSecret");
        secretField.setAccessible(true);
        secretField.set(tokenProvider, TEST_SECRET);
    }

    // --- Happy path: refresh token accepted ---
    @Test
    void refreshAccessToken_withValidRefreshToken_returnsNewAccessToken() {
        String refreshToken = tokenProvider.generateRefreshToken("user-001");
        String newAccessToken = tokenProvider.refreshAccessToken(refreshToken);
        assertNotNull(newAccessToken);

        Claims claims = tokenProvider.validateToken(newAccessToken);
        assertEquals("user-001", claims.getSubject());
        assertEquals("access", claims.get("type", String.class));
    }

    // --- Rejection: access token used as refresh token ---
    @Test
    void refreshAccessToken_withAccessToken_throwsSecurityException() {
        String accessToken = tokenProvider.generateAccessToken("user-001", "USER");
        assertThrows(SecurityException.class, () -> tokenProvider.refreshAccessToken(accessToken));
    }

    // --- Edge case: token with no type claim ---
    @Test
    void refreshAccessToken_withNoTypeClaim_throwsSecurityException() {
        // An access token has type="access", not "refresh", so it should be rejected
        String accessToken = tokenProvider.generateAccessToken("user-002", "ADMIN");
        SecurityException ex = assertThrows(SecurityException.class,
                () -> tokenProvider.refreshAccessToken(accessToken));
        assertEquals("Token is not a refresh token", ex.getMessage());
    }
}
