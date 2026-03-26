package com.finserv.middleware;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ErrorHandlerTest {

    private final ErrorHandler handler = new ErrorHandler();

    // === Issue #20: Generic error responses must not leak internals ===

    @Test
    @DisplayName("#20 happy path: generic error returns safe message")
    void handleGenericError_returnsSafeMessage() {
        ResponseEntity<Map<String, Object>> response =
            handler.handleGenericError(new RuntimeException("NullPointerException at com.finserv.internal.Foo"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("An internal error occurred. Please try again later.", body.get("message"));
    }

    @Test
    @DisplayName("#20 rejection: internal class names not exposed")
    void handleGenericError_doesNotExposeClassName() {
        ResponseEntity<Map<String, Object>> response =
            handler.handleGenericError(new IllegalStateException("Something broke"));

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.containsKey("exceptionType"), "exceptionType should not be in response");
        String message = (String) body.get("message");
        assertFalse(message.contains("IllegalStateException"));
        assertFalse(message.contains("com.finserv"));
    }

    @Test
    @DisplayName("#20 edge: SQL exception message not leaked")
    void handleGenericError_doesNotLeakSqlDetails() {
        ResponseEntity<Map<String, Object>> response =
            handler.handleGenericError(new RuntimeException("SELECT * FROM users WHERE id = 'admin'--"));

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse(((String) body.get("message")).contains("SELECT"));
    }
}
