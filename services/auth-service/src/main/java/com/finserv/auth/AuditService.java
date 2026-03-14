package com.finserv.auth;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Append-only audit trail for authentication events.
 * Implements SOX Section 404 compliance requirement (Issue #19).
 * No delete or update paths exist in this service by design.
 */
@Service
public class AuditService {

    private final CopyOnWriteArrayList<AuditEvent> auditLog = new CopyOnWriteArrayList<>();

    public void record(String eventType, String userId, String ipAddress,
                       String userAgent, String outcome) {
        auditLog.add(new AuditEvent(eventType, userId, ipAddress, userAgent, outcome, Instant.now()));
    }

    public List<AuditEvent> queryByUserId(String userId) {
        return auditLog.stream()
                .filter(e -> userId.equals(e.userId()))
                .collect(Collectors.toList());
    }

    public List<AuditEvent> queryByUserIdAndDateRange(String userId, Instant from, Instant to) {
        return auditLog.stream()
                .filter(e -> userId.equals(e.userId()))
                .filter(e -> !e.timestamp().isBefore(from) && !e.timestamp().isAfter(to))
                .collect(Collectors.toList());
    }

    public record AuditEvent(
            String eventType,
            String userId,
            String ipAddress,
            String userAgent,
            String outcome,
            Instant timestamp
    ) {}
}