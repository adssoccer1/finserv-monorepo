package com.finserv.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for authentication operations.
 * Base path: /api/v1/auth
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserAuthService userAuthService;
    private final JwtTokenProvider tokenProvider;

    public AuthController(UserAuthService userAuthService, JwtTokenProvider tokenProvider) {
        this.userAuthService = userAuthService;
        this.tokenProvider   = tokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody LoginRequest req,
            @RequestHeader(value = "X-Forwarded-For", defaultValue = "unknown") String ip,
            @RequestHeader(value = "User-Agent", defaultValue = "unknown") String userAgent) {

        UserAuthService.AuthResult result = userAuthService.login(
            req.email(), req.password(), ip, userAgent);

        if (!result.success()) {
            return ResponseEntity.status(401).body(Map.of(
                "error", result.errorCode()));
        }

        return ResponseEntity.ok(Map.of(
            "accessToken",  result.accessToken(),
            "refreshToken", result.refreshToken(),
            "sessionId",    result.sessionId(),
            "userId",       result.userId(),
            "role",         result.role()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "refreshToken is required"));
        }
        try {
            String newAccessToken = tokenProvider.refreshAccessToken(refreshToken);
            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired refresh token"));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestAttribute("userId") String userId,
            @RequestBody ChangePasswordRequest req) {
        try {
            userAuthService.changePassword(userId, req.currentPassword(), req.newPassword());
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "auth-service"));
    }

    public record LoginRequest(String email, String password) {}
    public record ChangePasswordRequest(String currentPassword, String newPassword) {}
}
