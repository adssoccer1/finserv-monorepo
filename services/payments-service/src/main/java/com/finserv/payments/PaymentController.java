package com.finserv.payments;

import com.finserv.utils.ErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * REST API for payment operations.
 * Base path: /api/v1/payments
 *
 * All endpoints require a valid Bearer token (enforced by AuthMiddleware).
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentProcessor  processor;
    private final PaymentRepository repository;

    public PaymentController(PaymentProcessor processor, PaymentRepository repository) {
        this.processor  = processor;
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> initiatePayment(
            @RequestBody PaymentRequest request,
            @RequestAttribute("userId") String requesterId) {

        PaymentProcessor.PaymentResult result = processor.process(request);

        if (!result.success()) {
            int status = result.errorCode().equals(ErrorCodes.PAYMENT_INSUFFICIENT_FUNDS) ? 422 : 400;
            return ResponseEntity.status(status).body(Map.of(
                "error", result.errorCode(),
                "message", result.errorMessage()
            ));
        }

        return ResponseEntity.status(201).body(Map.of(
            "paymentId", result.paymentId(),
            "amount",    result.amount(),
            "currency",  result.currency(),
            "status",    "COMPLETED"
        ));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<Map<String, Object>> getPayment(
            @PathVariable String paymentId,
            @RequestAttribute("userId") String requesterId) {

        Optional<PaymentRepository.PaymentRecord> record = repository.findById(paymentId);

        if (record.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                "error",   ErrorCodes.RESOURCE_NOT_FOUND,
                "message", "Payment not found: " + paymentId
            ));
        }

        PaymentRepository.PaymentRecord p = record.get();
        return ResponseEntity.ok(Map.of(
            "paymentId",            p.paymentId(),
            "sourceAccountId",      p.sourceAccountId(),
            "destinationAccountId", p.destinationAccountId(),
            "amount",               p.amount(),
            "currency",             p.currency(),
            "status",               p.status(),
            "memo",                 p.memo() != null ? p.memo() : "",
            "createdAt",            p.createdAt().toString()
        ));
    }

    @GetMapping("/account/{accountId}/balance")
    public ResponseEntity<Map<String, Object>> getBalance(
            @PathVariable String accountId,
            @RequestAttribute("userId") String requesterId) {

        long cents = repository.getBalanceCents(accountId);
        if (cents < 0) {
            return ResponseEntity.status(404).body(Map.of(
                "error", ErrorCodes.PAYMENT_ACCOUNT_NOT_FOUND,
                "message", "Account not found: " + accountId
            ));
        }

        BigDecimal balance = com.finserv.utils.CurrencyUtils.fromCents(cents);
        return ResponseEntity.ok(Map.of(
            "accountId",     accountId,
            "balanceUSD",    balance,
            "balanceCents",  cents
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "payments-service"));
    }
}
