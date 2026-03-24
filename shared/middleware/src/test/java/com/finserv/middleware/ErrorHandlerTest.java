package com.finserv.middleware;

import com.finserv.utils.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ErrorHandlerTest {

    private final ErrorHandler handler = new ErrorHandler();

    @Test
    void handleGenericError_returnsGenericMessage_doesNotLeakInternals() {
        RuntimeException ex = new RuntimeException("NullPointerException at com.finserv.payments.PaymentRepository.findById");

        ResponseEntity<Map<String, Object>> response = handler.handleGenericError(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCodes.INTERNAL_ERROR, body.get("code"));
        assertEquals("An unexpected error occurred. Please try again later.", body.get("message"));
        assertFalse(body.containsKey("exceptionType"), "Response must not contain exceptionType field");
        assertNotNull(body.get("timestamp"));
        // Verify no internal details leak in any field
        String bodyStr = body.toString();
        assertFalse(bodyStr.contains("com.finserv"), "Response must not contain internal class names");
        assertFalse(bodyStr.contains("NullPointerException"), "Response must not contain exception details");
    }

    @Test
    void handleGenericError_withSqlException_doesNotLeakQueryFragments() {
        Exception ex = new RuntimeException("ERROR: column \"password_hash\" does not exist - SQL state: 42703");

        ResponseEntity<Map<String, Object>> response = handler.handleGenericError(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        String bodyStr = body.toString();
        assertFalse(bodyStr.contains("password_hash"), "Response must not contain SQL fragments");
        assertFalse(bodyStr.contains("42703"), "Response must not contain SQL state codes");
    }

    @Test
    void handleValidationError_returnsUserMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("Amount must be positive");

        ResponseEntity<Map<String, Object>> response = handler.handleValidationError(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCodes.VALIDATION_ERROR, body.get("code"));
        assertEquals("Amount must be positive", body.get("message"));
    }

    @Test
    void handleSecurityError_returnsGenericAccessDenied() {
        SecurityException ex = new SecurityException("User user-001 attempted to access admin endpoint");

        ResponseEntity<Map<String, Object>> response = handler.handleSecurityError(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Access denied", body.get("message"));
        assertFalse(body.toString().contains("user-001"), "Response must not contain user IDs");
    }
}
