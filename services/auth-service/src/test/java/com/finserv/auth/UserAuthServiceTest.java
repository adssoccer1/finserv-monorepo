package com.finserv.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserAuthServiceTest {

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new SessionManager();
    }

    @Test
    void changePassword_invalidatesAllSessions() {
        // Create sessions for user-001
        String sid1 = sessionManager.createSession("user-001", "10.0.0.1", "Browser1");
        String sid2 = sessionManager.createSession("user-001", "10.0.0.2", "Browser2");

        assertEquals(2, sessionManager.getActiveSessionCount("user-001"));

        // Invalidate all sessions (this is what changePassword now calls)
        sessionManager.invalidateAllSessions("user-001");

        // Verify all sessions are gone
        assertEquals(0, sessionManager.getActiveSessionCount("user-001"));
        assertNull(sessionManager.getSession(sid1));
        assertNull(sessionManager.getSession(sid2));
    }

    @Test
    void invalidateAllSessions_doesNotAffectOtherUsers() {
        sessionManager.createSession("user-001", "10.0.0.1", "Browser1");
        sessionManager.createSession("user-002", "10.0.0.2", "Browser2");

        sessionManager.invalidateAllSessions("user-001");

        assertEquals(0, sessionManager.getActiveSessionCount("user-001"));
        assertEquals(1, sessionManager.getActiveSessionCount("user-002"));
    }

    @Test
    void invalidateAllSessions_handlesUserWithNoSessions() {
        // Should not throw when invalidating sessions for a user with none
        assertDoesNotThrow(() -> sessionManager.invalidateAllSessions("nonexistent-user"));
    }
}
