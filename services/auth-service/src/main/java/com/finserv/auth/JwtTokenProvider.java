package com.finserv.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates and validates JWT access tokens and refresh tokens.
 *
 * Token lifetimes:
 *   Access token:  15 minutes
 *   Refresh token: 30 days
 */
@Component
public class JwtTokenProvider {

    @Value("${finserv.jwt.secret}")
    private String jwtSecret;

    private static final long ACCESS_TOKEN_TTL_MS  = 15 * 60 * 1000L;       // 15 min
    private static final long REFRESH_TOKEN_TTL_MS = 30L * 24 * 60 * 60 * 1000L; // 30 days

    public String generateAccessToken(String userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("type", "access");
        return buildToken(userId, claims, ACCESS_TOKEN_TTL_MS);
    }

    public String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return buildToken(userId, claims, REFRESH_TOKEN_TTL_MS);
    }

    private String buildToken(String subject, Map<String, Object> claims, long ttlMs) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);
        return Jwts.builder()
                   .setClaims(claims)
                   .setSubject(subject)
                   .setIssuedAt(now)
                   .setExpiration(expiry)
                   .signWith(SignatureAlgorithm.HS256,
                             Base64.getDecoder().decode(jwtSecret))
                   .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
                   .setSigningKey(Base64.getDecoder().decode(jwtSecret))
                   .build()
                   .parseClaimsJws(token)
                   .getBody();
    }

    /**
     * Validates a refresh token and issues a new access token.
     * Rejects access tokens submitted as refresh tokens.
     */
    public String refreshAccessToken(String refreshToken) {
        Claims claims = validateToken(refreshToken);
        String tokenType = claims.get("type", String.class);
        if (!"refresh".equals(tokenType)) {
            throw new SecurityException("Invalid token type: expected refresh token");
        }
        String userId = claims.getSubject();
        String role   = claims.get("role", String.class);
        return generateAccessToken(userId, role != null ? role : "USER");
    }

    public String getUserIdFromToken(String token) {
        return validateToken(token).getSubject();
    }
}
