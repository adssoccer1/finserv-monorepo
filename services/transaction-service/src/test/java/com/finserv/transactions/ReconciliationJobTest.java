package com.finserv.transactions;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReconciliationJobTest {

    @Test
    void runNightlyReconciliation_usesUtcTimezone() {
        // Verify the fix: LocalDate.now(ZoneId.of("UTC")) is used
        // We test this by checking that the reconciliation processes transactions
        // based on UTC date, not system default
        TransactionRepository repository = new TransactionRepository();
        LedgerService ledgerService = new LedgerService(repository);
        ReconciliationJob job = new ReconciliationJob(repository, ledgerService);

        // Clear default data and add a transaction from "yesterday UTC"
        LocalDate yesterdayUtc = LocalDate.now(ZoneId.of("UTC")).minusDays(1);
        Instant yesterdayNoon = yesterdayUtc.atTime(12, 0).toInstant(ZoneOffset.UTC);

        repository.save(new TransactionRepository.TransactionRecord(
            "TXN-UTC-TEST", "ACCT-001", "DEBIT",
            new BigDecimal("100.00"), "USD", "COMPLETED",
            "PAY-TEST", "UTC test transaction",
            yesterdayNoon, false));

        // Run reconciliation — should process the transaction using UTC date
        job.runNightlyReconciliation();

        // Verify the transaction was reconciled
        assertTrue(repository.findById("TXN-UTC-TEST").isPresent());
        assertTrue(repository.findById("TXN-UTC-TEST").get().reconciled(),
            "Transaction from yesterday UTC should be reconciled");
    }

    @Test
    void runNightlyReconciliation_doesNotProcessTodayTransactions() {
        TransactionRepository repository = new TransactionRepository();
        LedgerService ledgerService = new LedgerService(repository);
        ReconciliationJob job = new ReconciliationJob(repository, ledgerService);

        // Add a transaction from "today UTC"
        Instant todayNoon = LocalDate.now(ZoneId.of("UTC")).atTime(12, 0).toInstant(ZoneOffset.UTC);

        repository.save(new TransactionRepository.TransactionRecord(
            "TXN-TODAY-TEST", "ACCT-001", "DEBIT",
            new BigDecimal("50.00"), "USD", "COMPLETED",
            "PAY-TODAY", "Today's transaction",
            todayNoon, false));

        job.runNightlyReconciliation();

        // Today's transaction should NOT be reconciled
        assertTrue(repository.findById("TXN-TODAY-TEST").isPresent());
        assertFalse(repository.findById("TXN-TODAY-TEST").get().reconciled(),
            "Transaction from today should not be reconciled by nightly job");
    }

    @Test
    void runNightlyReconciliation_handlesEmptyTransactionList() {
        TransactionRepository repository = new TransactionRepository();
        LedgerService ledgerService = new LedgerService(repository);
        ReconciliationJob job = new ReconciliationJob(repository, ledgerService);

        // Mark all pre-populated transactions as reconciled so none match
        repository.findById("TXN-00000001").ifPresent(t ->
            repository.markReconciled(t.transactionId()));
        repository.findById("TXN-00000002").ifPresent(t ->
            repository.markReconciled(t.transactionId()));
        repository.findById("TXN-00000003").ifPresent(t ->
            repository.markReconciled(t.transactionId()));
        repository.findById("TXN-00000004").ifPresent(t ->
            repository.markReconciled(t.transactionId()));

        // Should run without error even with no unreconciled transactions
        assertDoesNotThrow(() -> job.runNightlyReconciliation());
    }
}
