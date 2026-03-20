package com.finserv.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private String base64Secret;

    @BeforeEach
    void setUp() throws Exception {
        tokenProvider = new JwtTokenProvider();
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        base64Secret = Base64.getEncoder().encodeToString(key.getEncoded());
        Field secretField = JwtTokenProvider.class.getDeclaredField("jwtSecret");
        secretField.setAccessible(true);
        secretField.set(tokenProvider, base64Secret);
    }

    @Test
    void refreshAccessToken_acceptsRefreshToken() {
        String refreshToken = tokenProvider.generateRefreshToken("user-001");
        String newAccessToken = tokenProvider.refreshAccessToken(refreshToken);
        assertNotNull(newAccessToken);

        Claims claims = tokenProvider.validateToken(newAccessToken);
        assertEquals("user-001", claims.getSubject());
        assertEquals("access", claims.get("type", String.class));
    }

    @Test
    void refreshAccessToken_rejectsAccessToken() {
        String accessToken = tokenProvider.generateAccessToken("user-001", "USER");
        assertThrows(SecurityException.class, () ->
            tokenProvider.refreshAccessToken(accessToken));
    }

    @Test
    void refreshAccessToken_rejectsTokenWithNoTypeClaim() {
        // Build a token with no type claim
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");
        String tokenNoType = Jwts.builder()
            .setClaims(claims)
            .setSubject("user-001")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 60_000))
            .signWith(SignatureAlgorithm.HS256,
                      Base64.getDecoder().decode(base64Secret))
            .compact();

        assertThrows(SecurityException.class, () ->
            tokenProvider.refreshAccessToken(tokenNoType));
    }
}
