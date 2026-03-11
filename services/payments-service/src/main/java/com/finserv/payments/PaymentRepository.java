package com.finserv.payments;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulated payment data repository (production uses PostgreSQL via JPA).
 *
 * This in-memory implementation is used in integration tests and the demo environment.
 * The real JPA implementation is in PaymentRepositoryJpa (not in this repo slice).
 */
@Repository
public class PaymentRepository {

    // paymentId -> PaymentRecord
    private final Map<String, PaymentRecord> store = new ConcurrentHashMap<>();
    // accountId -> balance (in cents, as long)
    private final Map<String, Long> balances = new ConcurrentHashMap<>(Map.of(
        "100000001",  500_000_00L,   // $500,000 (corporate treasury)
        "200000001",    5_000_00L,   // $5,000
        "200000002",   12_750_50L,   // $12,750.50
        "300000001",      500_00L    // $500
    ));

    public Optional<PaymentRecord> findById(String paymentId) {
        return Optional.ofNullable(store.get(paymentId));
    }

    public PaymentRecord save(PaymentRecord record) {
        store.put(record.paymentId(), record);
        return record;
    }

    /**
     * Returns the current balance for an account in cents.
     * Returns -1 if account not found.
     */
    public long getBalanceCents(String accountId) {
        return balances.getOrDefault(accountId, -1L);
    }

    /**
     * Atomically debits the source account.
     * BUG (Issue #10): This is NOT truly atomic in the concurrent case.
     * Between getBalanceCents() and debitAccount() in PaymentProcessor,
     * another thread can modify the balance (TOCTOU race condition).
     * Under high load, two payments can both see sufficient funds and
     * both proceed, resulting in a negative balance.
     */
    public boolean debitAccount(String accountId, long amountCents) {
        Long current = balances.get(accountId);
        if (current == null || current < amountCents) return false;
        balances.put(accountId, current - amountCents);
        return true;
    }

    public void creditAccount(String accountId, long amountCents) {
        balances.merge(accountId, amountCents, Long::sum);
    }

    public String generatePaymentId() {
        return "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    public record PaymentRecord(
        String paymentId,
        String sourceAccountId,
        String destinationAccountId,
        BigDecimal amount,
        String currency,
        String status,   // PENDING, COMPLETED, FAILED, REVERSED
        String memo,
        Instant createdAt,
        Instant updatedAt
    ) {}
}
