package com.finserv.auth;

import com.finserv.utils.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active user sessions (in-memory store).
 * In production this is backed by Redis — this class wraps the cache client.
 *
 * Session TTL: 30 days (matches refresh token lifetime).
 */
@Service
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
    private static final long SESSION_TTL_SECONDS = 30L * 24 * 60 * 60;

    // sessionId -> SessionInfo
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    // userId -> set of sessionIds
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    public String createSession(String userId, String ipAddress, String userAgent) {
        String sessionId = UUID.randomUUID().toString();
        SessionInfo info = new SessionInfo(userId, ipAddress, userAgent,
                                          Instant.now().plusSeconds(SESSION_TTL_SECONDS));
        sessions.put(sessionId, info);
        userSessions.computeIfAbsent(userId, id -> ConcurrentHashMap.newKeySet()).add(sessionId);
        log.info("Session created for user {} from {}", userId, ipAddress);
        return sessionId;
    }

    public SessionInfo getSession(String sessionId) {
        SessionInfo info = sessions.get(sessionId);
        if (info == null) return null;
        if (Instant.now().isAfter(info.expiresAt())) {
            sessions.remove(sessionId);
            return null;
        }
        return info;
    }

    public void invalidateSession(String sessionId) {
        SessionInfo info = sessions.remove(sessionId);
        if (info != null) {
            Set<String> userSids = userSessions.get(info.userId());
            if (userSids != null) userSids.remove(sessionId);
            log.info("Session {} invalidated for user {}", sessionId, info.userId());
        }
    }

    /**
     * Invalidates ALL sessions for a given user.
     * Should be called on password change and account lock.
     */
    public void invalidateAllSessions(String userId) {
        Set<String> sids = userSessions.remove(userId);
        if (sids != null) {
            sids.forEach(sessions::remove);
            log.info("All {} sessions invalidated for user {}", sids.size(), userId);
        }
    }

    public int getActiveSessionCount(String userId) {
        Set<String> sids = userSessions.getOrDefault(userId, Set.of());
        return (int) sids.stream().filter(sid -> getSession(sid) != null).count();
    }

    public record SessionInfo(String userId, String ipAddress, String userAgent, Instant expiresAt) {}
}
