package com.finserv.middleware;

import com.finserv.utils.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Global exception handler for all REST controllers.
 *
 * SECURITY BUG (Issue #20): The generic exception handler returns e.getMessage()
 * directly in the API response. For low-level exceptions (SQL errors, NullPointerExceptions
 * from internal services), this can leak internal stack traces, class names, and
 * query fragments to API consumers — violating OWASP A05:2021.
 *
 * Discovered during a PEN test by TrustPillar Security, 2024-01-18.
 */
@RestControllerAdvice
public class ErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of(
            "code",      ErrorCodes.VALIDATION_ERROR,
            "message",   e.getMessage(),
            "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityError(SecurityException e) {
        log.warn("Security violation: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
            "code",      ErrorCodes.AUTH_TOKEN_INVALID,
            "message",   "Access denied",
            "timestamp", Instant.now().toString()
        ));
    }

    /**
     * BUG: Catches all exceptions and returns e.getMessage() and e.getClass().getName()
     * in the response body. For internal runtime exceptions this exposes:
     *  - Internal class names (e.g. "com.finserv.payments.PaymentRepository")
     *  - SQL fragments from JPA exceptions
     *  - NullPointerException at com.finserv...
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "code",          ErrorCodes.INTERNAL_ERROR,
            "message",       e.getMessage(),            // <-- leaks internal error detail
            "exceptionType", e.getClass().getName(),    // <-- leaks internal class names
            "timestamp",     Instant.now().toString()
        ));
    }
}
