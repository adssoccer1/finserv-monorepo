package com.finserv.transactions;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Simulated transaction data repository.
 * Production uses PostgreSQL with a partitioned transactions table (partitioned by month).
 */
@Repository
public class TransactionRepository {

    private final Map<String, TransactionRecord> store = new ConcurrentHashMap<>();

    public TransactionRepository() {
        // Pre-populate with realistic test data
        Instant now = Instant.now();
        save(new TransactionRecord("TXN-00000001", "200000001", "DEBIT",
            new BigDecimal("250.00"), "USD", "COMPLETED",
            "PAY-AABBCCDD11223344", "Rent payment - March",
            now.minusSeconds(86400 * 2), false));
        save(new TransactionRecord("TXN-00000002", "200000001", "CREDIT",
            new BigDecimal("3200.00"), "USD", "COMPLETED",
            null, "Payroll deposit",
            now.minusSeconds(86400 * 5), false));
        save(new TransactionRecord("TXN-00000003", "200000002", "DEBIT",
            new BigDecimal("1500.00"), "USD", "COMPLETED",
            "PAY-EEFF00112233AABB", "Wire transfer - supplier",
            now.minusSeconds(86400 * 1), false));
        save(new TransactionRecord("TXN-00000004", "300000001", "DEBIT",
            new BigDecimal("45.99"), "USD", "COMPLETED",
            "PAY-11223344AABBCCDD", "Monthly fee",
            now.minusSeconds(3600), false));
    }

    public Optional<TransactionRecord> findById(String transactionId) {
        return Optional.ofNullable(store.get(transactionId));
    }

    public TransactionRecord save(TransactionRecord record) {
        store.put(record.transactionId(), record);
        return record;
    }

    /**
     * Returns transactions for an account, sorted by timestamp descending.
     * Supports basic pagination via offset/limit.
     *
     * BUG (Issue #16 - ambiguous): When pageSize is not passed by callers,
     * this returns ALL transactions with no limit — potentially thousands of rows.
     * Related customer complaint: "transaction history loads slowly for older accounts."
     */
    public List<TransactionRecord> findByAccountId(String accountId, int offset, int limit) {
        return store.values().stream()
            .filter(t -> t.accountId().equals(accountId))
            .sorted(Comparator.comparing(TransactionRecord::createdAt).reversed())
            .skip(offset)
            .limit(limit > 0 ? limit : Long.MAX_VALUE)   // BUG: limit=0 means no limit
            .collect(Collectors.toList());
    }

    /**
     * Returns all unreconciled transactions created between two instants.
     *
     * BUG (Issue #14 - medium): Uses createdAt range on the in-memory store.
     * In the JPA version, the query uses LocalDate.now() without timezone,
     * which on UTC+0 servers during daylight saving transition skips the
     * 1-hour window of transactions. See ReconciliationJob for the related bug.
     */
    public List<TransactionRecord> findUnreconciledInRange(Instant from, Instant to) {
        return store.values().stream()
            .filter(t -> !t.reconciled())
            .filter(t -> !t.createdAt().isBefore(from) && t.createdAt().isBefore(to))
            .collect(Collectors.toList());
    }

    public void markReconciled(String transactionId) {
        TransactionRecord existing = store.get(transactionId);
        if (existing != null) {
            store.put(transactionId, new TransactionRecord(
                existing.transactionId(), existing.accountId(), existing.type(),
                existing.amount(), existing.currency(), existing.status(),
                existing.referencePaymentId(), existing.description(),
                existing.createdAt(), true));
        }
    }

    public String generateTransactionId() {
        return String.format("TXN-%08d", store.size() + 1);
    }

    public record TransactionRecord(
        String     transactionId,
        String     accountId,
        String     type,              // DEBIT or CREDIT
        BigDecimal amount,
        String     currency,
        String     status,            // PENDING, COMPLETED, FAILED, REVERSED
        String     referencePaymentId,
        String     description,
        Instant    createdAt,
        boolean    reconciled
    ) {}
}
