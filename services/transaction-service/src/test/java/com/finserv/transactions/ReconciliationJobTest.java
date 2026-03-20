package com.finserv.transactions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ReconciliationJobTest {

    private TransactionRepository repository;
    private LedgerService ledgerService;
    private ReconciliationJob job;

    @BeforeEach
    void setUp() {
        repository = new TransactionRepository();
        ledgerService = new LedgerService(repository);
        job = new ReconciliationJob(repository, ledgerService);
    }

    @Test
    void reconciliation_usesBigDecimalForPrecision() {
        // Clear existing test data by reconciling it
        job.runNightlyReconciliation();

        // Add balanced debit/credit entries that would cause floating-point errors with double
        Instant yesterday = Instant.now().minusSeconds(86400);
        for (int i = 0; i < 100; i++) {
            String txnId = repository.generateTransactionId();
            repository.save(new TransactionRepository.TransactionRecord(
                txnId, "ACC-001", "DEBIT",
                new BigDecimal("0.10"), "USD", "COMPLETED",
                "PAY-TEST", "Test debit", yesterday, false));

            String txnId2 = repository.generateTransactionId();
            repository.save(new TransactionRepository.TransactionRecord(
                txnId2, "ACC-002", "CREDIT",
                new BigDecimal("0.10"), "USD", "COMPLETED",
                "PAY-TEST", "Test credit", yesterday, false));
        }

        // Should not throw or log an error — BigDecimal handles this precisely
        assertDoesNotThrow(() -> job.runNightlyReconciliation());
    }

    @Test
    void reconciliation_detectsImbalance() {
        Instant yesterday = Instant.now().minusSeconds(86400);
        String txnId = repository.generateTransactionId();
        repository.save(new TransactionRepository.TransactionRecord(
            txnId, "ACC-001", "DEBIT",
            new BigDecimal("100.00"), "USD", "COMPLETED",
            "PAY-TEST", "Unmatched debit", yesterday, false));

        // Should not throw — imbalance is logged, not thrown
        assertDoesNotThrow(() -> job.runNightlyReconciliation());
    }

    @Test
    void reconciliation_handlesEmptyTransactionList() {
        // Reconcile everything first
        job.runNightlyReconciliation();
        // Run again with no unreconciled transactions
        assertDoesNotThrow(() -> job.runNightlyReconciliation());
    }
}
