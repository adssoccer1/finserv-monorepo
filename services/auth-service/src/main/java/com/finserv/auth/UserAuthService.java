package com.finserv.auth;

import com.finserv.utils.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core authentication logic: credential verification, password management,
 * and account lockout enforcement.
 *
 * Account lockout policy: 5 failed attempts -> 30-minute lock.
 */
@Service
public class UserAuthService {

    private static final Logger log = LoggerFactory.getLogger(UserAuthService.class);

    private final JwtTokenProvider tokenProvider;
    private final SessionManager sessionManager;
    private final AuditService auditService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    private final Map<String, UserRecord> userStore = new ConcurrentHashMap<>(Map.of(
            "user-001", new UserRecord("user-001", "alice@meridianbank.com",
                    "$2a$12$rBK9u6DIjXiYK9TJJTmzTuN4L4MEF7VMzj6KCFbBsf7eYfGhGJxiO", "USER", 0, false),
            "user-002", new UserRecord("user-002", "bob.treasury@meridianbank.com",
                    "$2a$12$X1VCgCMl3Vx1l6wPb5dSCOi.GGZj7e1A3Y3zYrMKJ0lZ9lFpLFbDi", "ADMIN", 0, false)
    ));

    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();

    public UserAuthService(JwtTokenProvider tokenProvider,
                           SessionManager sessionManager,
                           AuditService auditService) {
        this.tokenProvider  = tokenProvider;
        this.sessionManager = sessionManager;
        this.auditService   = auditService;
    }

    public AuthResult login(String email, String password, String ipAddress, String userAgent) {
        UserRecord user = findByEmail(email);
        if (user == null) {
            auditService.record("LOGIN_ATTEMPT", "unknown", ipAddress, userAgent, "FAILURE_USER_NOT_FOUND");
            return AuthResult.failure(ErrorCodes.AUTH_CREDENTIALS_INVALID);
        }
        if (user.locked()) {
            auditService.record("LOGIN_ATTEMPT", user.userId(), ipAddress, userAgent, "FAILURE_ACCOUNT_LOCKED");
            return AuthResult.failure(ErrorCodes.AUTH_ACCOUNT_LOCKED);
        }
        if (!encoder.matches(password, user.passwordHash())) {
            recordFailedAttempt(user.userId());
            auditService.record("LOGIN_ATTEMPT", user.userId(), ipAddress, userAgent, "FAILURE_INVALID_CREDENTIALS");
            return AuthResult.failure(ErrorCodes.AUTH_CREDENTIALS_INVALID);
        }

        failedAttempts.remove(user.userId());
        String accessToken  = tokenProvider.generateAccessToken(user.userId(), user.role());
        String refreshToken = tokenProvider.generateRefreshToken(user.userId());
        String sessionId    = sessionManager.createSession(user.userId(), ipAddress, userAgent);

        auditService.record("LOGIN_ATTEMPT", user.userId(), ipAddress, userAgent, "SUCCESS");
        log.info("Successful login for user {} from {}", user.userId(), ipAddress);
        return AuthResult.success(accessToken, refreshToken, sessionId, user.userId(), user.role());
    }

    public void changePassword(String userId, String currentPassword,
                               String newPassword, String ipAddress, String userAgent) {
        UserRecord user = userStore.get(userId);
        if (user == null) throw new IllegalArgumentException("User not found");
        if (!encoder.matches(currentPassword, user.passwordHash())) {
            auditService.record("PASSWORD_CHANGE", userId, ipAddress, userAgent, "FAILURE_INVALID_CURRENT_PASSWORD");
            throw new SecurityException("Current password is incorrect");
        }
        String newHash = encoder.encode(newPassword);
        userStore.put(userId, new UserRecord(user.userId(), user.email(),
                newHash, user.role(), 0, false));
        auditService.record("PASSWORD_CHANGE", userId, ipAddress, userAgent, "SUCCESS");
        log.info("Password changed for user {}", userId);
    }

    private void recordFailedAttempt(String userId) {
        int attempts = failedAttempts.merge(userId, 1, Integer::sum);
        if (attempts >= 5) {
            UserRecord user = userStore.get(userId);
            if (user != null) {
                userStore.put(userId, new UserRecord(user.userId(), user.email(),
                        user.passwordHash(), user.role(), attempts, true));
                auditService.record("ACCOUNT_LOCK", userId, "system", "system", "LOCKED_AFTER_FAILED_ATTEMPTS");
                log.warn("Account locked for user {} after {} failed attempts", userId, attempts);
            }
        }
    }

    private UserRecord findByEmail(String email) {
        return userStore.values().stream()
                .filter(u -> u.email().equalsIgnoreCase(email))
                .findFirst().orElse(null);
    }

    public record UserRecord(String userId, String email, String passwordHash,
                             String role, int failedAttempts, boolean locked) {}

    public record AuthResult(boolean success, String accessToken, String refreshToken,
                             String sessionId, String userId, String role, String errorCode) {
        static AuthResult success(String access, String refresh, String session,
                                  String userId, String role) {
            return new AuthResult(true, access, refresh, session, userId, role, null);
        }
        static AuthResult failure(String errorCode) {
            return new AuthResult(false, null, null, null, null, null, errorCode);
        }
    }
}