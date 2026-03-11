package com.finserv.notifications;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for triggering notifications (internal use only).
 * This endpoint is not exposed externally — called by other services via
 * service-to-service auth within the VPC.
 *
 * Base path: /internal/v1/notifications
 */
@RestController
@RequestMapping("/internal/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/payment-alert")
    public ResponseEntity<Map<String, String>> sendPaymentAlert(
            @RequestBody PaymentAlertRequest req) {

        try {
            notificationService.sendPaymentAlert(
                req.userId(), req.email(), req.phoneNumber(),
                req.accountId(), req.amount(), req.currency(),
                req.transactionId(), req.isSender());

            return ResponseEntity.ok(Map.of("status", "queued"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/login-alert")
    public ResponseEntity<Map<String, String>> sendLoginAlert(
            @RequestBody LoginAlertRequest req) {

        notificationService.sendAlert(
            req.userId(), req.email(), req.phoneNumber(),
            AlertTemplateManager.AlertType.LOGIN_NEW_DEVICE,
            Map.of("ipAddress", req.ipAddress(), "timestamp", req.timestamp()));

        return ResponseEntity.ok(Map.of("status", "queued"));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "notification-service"));
    }

    public record PaymentAlertRequest(String userId, String email, String phoneNumber,
                                       String accountId, String amount, String currency,
                                       String transactionId, boolean isSender) {}

    public record LoginAlertRequest(String userId, String email, String phoneNumber,
                                     String ipAddress, String timestamp) {}
}
