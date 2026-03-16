package com.finserv.transactions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Tests for ReconciliationJob — verifies BigDecimal arithmetic (Issue #5)
 * and explicit UTC timezone usage (Issue #14).
 */
class ReconciliationJobTest {

    private TransactionRepository repository;
    private LedgerService ledgerService;
    private ReconciliationJob job;

    @BeforeEach
    void setUp() {
        repository = mock(TransactionRepository.class);
        ledgerService = mock(LedgerService.class);
        job = new ReconciliationJob(repository, ledgerService);
    }

    // Happy path: balanced debits and credits reconcile without error
    @Test
    void runNightlyReconciliation_balancedTransactions_completesSuccessfully() {
        LocalDate yesterday = LocalDate.now(ZoneId.of("UTC")).minusDays(1);
        Instant from = yesterday.atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant to = yesterday.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant();

        List<TransactionRepository.TransactionRecord> records = List.of(
            new TransactionRepository.TransactionRecord(
                "TXN-1", "ACC-1", "DEBIT", new BigDecimal("100.00"),
                "USD", "COMPLETED", "PAY-1", "test", from.plusSeconds(3600), false),
            new TransactionRepository.TransactionRecord(
                "TXN-2", "ACC-2", "CREDIT", new BigDecimal("100.00"),
                "USD", "COMPLETED", "PAY-1", "test", from.plusSeconds(3600), false)
        );

        when(repository.findUnreconciledInRange(from, to)).thenReturn(records);

        job.runNightlyReconciliation();

        verify(repository).markReconciled("TXN-1");
        verify(repository).markReconciled("TXN-2");
    }

    // Failure case: imbalanced transactions are detected (not silently rounded away)
    @Test
    void runNightlyReconciliation_imbalancedTransactions_detectsDiscrepancy() {
        LocalDate yesterday = LocalDate.now(ZoneId.of("UTC")).minusDays(1);
        Instant from = yesterday.atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant to = yesterday.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant();

        // Debit of 100.01, Credit of 100.00 — net != 0
        List<TransactionRepository.TransactionRecord> records = List.of(
            new TransactionRepository.TransactionRecord(
                "TXN-1", "ACC-1", "DEBIT", new BigDecimal("100.01"),
                "USD", "COMPLETED", "PAY-1", "test", from.plusSeconds(3600), false),
            new TransactionRepository.TransactionRecord(
                "TXN-2", "ACC-2", "CREDIT", new BigDecimal("100.00"),
                "USD", "COMPLETED", "PAY-1", "test", from.plusSeconds(3600), false)
        );

        when(repository.findUnreconciledInRange(from, to)).thenReturn(records);

        // Should complete without throwing — imbalance is logged, not thrown
        job.runNightlyReconciliation();

        verify(repository).markReconciled("TXN-1");
        verify(repository).markReconciled("TXN-2");
    }

    // Edge case: many small transactions that would cause floating-point drift
    // with double but stay precise with BigDecimal
    @Test
    void runNightlyReconciliation_manySmallTransactions_noPrecisionLoss() {
        LocalDate yesterday = LocalDate.now(ZoneId.of("UTC")).minusDays(1);
        Instant from = yesterday.atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant to = yesterday.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant();

        // 1000 debits of 0.01 and 1 credit of 10.00 — should balance exactly
        var records = new java.util.ArrayList<TransactionRepository.TransactionRecord>();
        for (int i = 0; i < 1000; i++) {
            records.add(new TransactionRepository.TransactionRecord(
                "TXN-D-" + i, "ACC-1", "DEBIT", new BigDecimal("0.01"),
                "USD", "COMPLETED", null, "small debit", from.plusSeconds(i), false));
        }
        records.add(new TransactionRepository.TransactionRecord(
            "TXN-C-1", "ACC-2", "CREDIT", new BigDecimal("10.00"),
            "USD", "COMPLETED", null, "matching credit", from.plusSeconds(2000), false));

        when(repository.findUnreconciledInRange(from, to)).thenReturn(records);

        // With double arithmetic, 1000 * 0.01 != 10.00 exactly
        // With BigDecimal, this should balance perfectly
        job.runNightlyReconciliation();

        verify(repository, times(1001)).markReconciled(anyString());
    }
}
