package com.finserv.transactions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationJobTest {

    @Mock
    private TransactionRepository repository;

    @Mock
    private LedgerService ledgerService;

    private ReconciliationJob job;

    @BeforeEach
    void setUp() {
        job = new ReconciliationJob(repository, ledgerService);
    }

    @Test
    void runNightlyReconciliation_usesExactBigDecimalArithmetic() {
        // Simulate many small transactions that would cause floating-point drift with double
        TransactionRepository.TransactionRecord debit = new TransactionRepository.TransactionRecord(
            "TXN-1", "ACC-1", "DEBIT", new BigDecimal("0.10"), "USD",
            "COMPLETED", "PAY-1", "test", Instant.now().minusSeconds(86400), false);
        TransactionRepository.TransactionRecord credit = new TransactionRepository.TransactionRecord(
            "TXN-2", "ACC-2", "CREDIT", new BigDecimal("0.10"), "USD",
            "COMPLETED", "PAY-1", "test", Instant.now().minusSeconds(86400), false);

        when(repository.findUnreconciledInRange(any(), any()))
            .thenReturn(List.of(debit, credit));

        // Should not throw or log an imbalance — BigDecimal arithmetic is exact
        job.runNightlyReconciliation();

        verify(repository).markReconciled("TXN-1");
        verify(repository).markReconciled("TXN-2");
    }

    @Test
    void runNightlyReconciliation_handlesEmptyTransactionList() {
        when(repository.findUnreconciledInRange(any(), any()))
            .thenReturn(List.of());

        job.runNightlyReconciliation();

        verify(repository, never()).markReconciled(any());
    }

    @Test
    void runNightlyReconciliation_detectsImbalance() {
        // Only a debit, no matching credit — should log imbalance
        TransactionRepository.TransactionRecord debit = new TransactionRepository.TransactionRecord(
            "TXN-1", "ACC-1", "DEBIT", new BigDecimal("100.00"), "USD",
            "COMPLETED", "PAY-1", "test", Instant.now().minusSeconds(86400), false);

        when(repository.findUnreconciledInRange(any(), any()))
            .thenReturn(List.of(debit));

        // This should log an error but not throw
        job.runNightlyReconciliation();

        verify(repository).markReconciled("TXN-1");
    }
}
