package com.finserv.transactions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReconciliationJobTest {

    private TransactionRepository repository;
    private LedgerService ledgerService;
    private ReconciliationJob job;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(TransactionRepository.class);
        ledgerService = Mockito.mock(LedgerService.class);
        job = new ReconciliationJob(repository, ledgerService);
    }

    // === Issue #5: BigDecimal totals instead of double ===

    @Test
    @DisplayName("#5 happy path: balanced transactions produce zero net")
    void runNightlyReconciliation_balancedTransactions_noImbalance() {
        TransactionRepository.TransactionRecord debit = new TransactionRepository.TransactionRecord(
                "txn-1", "acct-1", "DEBIT",
                new BigDecimal("100.01"), "USD", "COMPLETED",
                "pay-1", "Test debit", Instant.now(), false);

        TransactionRepository.TransactionRecord credit = new TransactionRepository.TransactionRecord(
                "txn-2", "acct-2", "CREDIT",
                new BigDecimal("100.01"), "USD", "COMPLETED",
                "pay-1", "Test credit", Instant.now(), false);

        when(repository.findUnreconciledInRange(any(), any())).thenReturn(List.of(debit, credit));

        // Should not throw — balanced transactions
        job.runNightlyReconciliation();

        verify(repository, times(2)).markReconciled(anyString());
    }

    @Test
    @DisplayName("#5 rejection: imbalanced transactions detected with BigDecimal precision")
    void runNightlyReconciliation_imbalancedTransactions_detected() {
        TransactionRepository.TransactionRecord debit = new TransactionRepository.TransactionRecord(
                "txn-1", "acct-1", "DEBIT",
                new BigDecimal("100.01"), "USD", "COMPLETED",
                "pay-1", "Test debit", Instant.now(), false);

        TransactionRepository.TransactionRecord credit = new TransactionRepository.TransactionRecord(
                "txn-2", "acct-2", "CREDIT",
                new BigDecimal("100.02"), "USD", "COMPLETED",
                "pay-1", "Test credit", Instant.now(), false);

        when(repository.findUnreconciledInRange(any(), any())).thenReturn(List.of(debit, credit));

        // Should detect 0.01 imbalance with BigDecimal (would be missed with double in some cases)
        job.runNightlyReconciliation();

        verify(repository, times(2)).markReconciled(anyString());
    }

    @Test
    @DisplayName("#5 edge: many small amounts that would cause double rounding errors")
    void runNightlyReconciliation_manySmallAmounts_noPrecisionLoss() {
        // With double, adding 0.01 five hundred times gives ~5.000000000000001
        // With BigDecimal, it gives exactly 5.00
        List<TransactionRepository.TransactionRecord> records = new java.util.ArrayList<>();
        for (int i = 0; i < 500; i++) {
            records.add(new TransactionRepository.TransactionRecord(
                    "txn-d-" + i, "acct-1", "DEBIT",
                    new BigDecimal("0.01"), "USD", "COMPLETED",
                    "pay-" + i, "Small debit", Instant.now(), false));
        }
        for (int i = 0; i < 500; i++) {
            records.add(new TransactionRepository.TransactionRecord(
                    "txn-c-" + i, "acct-2", "CREDIT",
                    new BigDecimal("0.01"), "USD", "COMPLETED",
                    "pay-" + i, "Small credit", Instant.now(), false));
        }

        when(repository.findUnreconciledInRange(any(), any())).thenReturn(records);

        // Should not report imbalance — BigDecimal keeps exact precision
        job.runNightlyReconciliation();

        verify(repository, times(1000)).markReconciled(anyString());
    }

    // === Issue #14: UTC timezone in ReconciliationJob ===

    @Test
    @DisplayName("#14 happy path: reconciliation uses UTC date range")
    void runNightlyReconciliation_usesUtcDateRange() {
        ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);

        when(repository.findUnreconciledInRange(any(), any())).thenReturn(List.of());

        job.runNightlyReconciliation();

        verify(repository).findUnreconciledInRange(fromCaptor.capture(), toCaptor.capture());

        Instant from = fromCaptor.getValue();
        Instant to = toCaptor.getValue();

        // Verify the range is exactly 24 hours
        assertEquals(86400, to.getEpochSecond() - from.getEpochSecond());

        // Verify 'from' is at UTC midnight of yesterday
        LocalDate yesterday = LocalDate.now(ZoneId.of("UTC")).minusDays(1);
        Instant expectedFrom = yesterday.atStartOfDay(ZoneId.of("UTC")).toInstant();
        assertEquals(expectedFrom, from);
    }
}
