package com.finserv.middleware;

import com.finserv.utils.ErrorCodes;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Base64;
import java.util.Set;
import java.util.Collections;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.security.Keys;

/**
 * Validates Bearer tokens on all protected routes.
 *
 * BUG (Issue #12 - medium/security): The JWT parser does not explicitly set
 * a list of allowed signing algorithms. An attacker who controls a token can
 * set alg=none and bypass signature verification entirely.
 *
 * Fix: use .setAllowedClockSkewSeconds() and explicitly specify algorithm
 * via .verifyWith() or setSigningKey() with algorithm enforcement.
 */
@Component
@Order(2)
public class AuthMiddleware extends OncePerRequestFilter {

    @Value("${finserv.jwt.secret}")
    private String jwtSecret;

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<String> ALLOWED_ALGORITHMS = Collections.singleton("HS256");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ErrorCodes.AUTH_TOKEN_INVALID);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret));
            Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);

            String algorithm = jws.getHeader().getAlgorithm();
            if (!ALLOWED_ALGORITHMS.contains(algorithm)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ErrorCodes.AUTH_TOKEN_INVALID);
                return;
            }

            Claims claims = jws.getBody();
            request.setAttribute("userId",   claims.getSubject());
            request.setAttribute("userRole", claims.get("role", String.class));
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ErrorCodes.AUTH_TOKEN_INVALID);
        }
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/actuator/health")
            || path.startsWith("/api/v1/auth/login")
            || path.startsWith("/api/v1/auth/register");
    }
}
