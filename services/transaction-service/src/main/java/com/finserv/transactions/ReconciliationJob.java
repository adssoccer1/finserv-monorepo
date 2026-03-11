package com.finserv.transactions;

import com.finserv.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Nightly reconciliation job. Runs at 01:00 UTC, processes all unreconciled
 * transactions from the previous calendar day and verifies the ledger balance.
 *
 * BUG (Issue #5 - small): Uses double arithmetic to compute running totals
 * instead of BigDecimal. For large transaction volumes this accumulates
 * floating-point rounding errors, causing penny discrepancies in reconciliation
 * reports. Observed in production for accounts with > 500 transactions/day.
 *
 * BUG (Issue #14 - medium): LocalDate.now() uses the system default timezone
 * (UTC on prod servers, but EST on legacy batch servers), causing the job to
 * skip or double-process transactions created near midnight UTC during the
 * daylight saving transition window.
 */
@Component
public class ReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationJob.class);

    private final TransactionRepository repository;
    private final LedgerService         ledgerService;

    public ReconciliationJob(TransactionRepository repository, LedgerService ledgerService) {
        this.repository    = repository;
        this.ledgerService = ledgerService;
    }

    @Scheduled(cron = "0 0 1 * * *")  // 01:00 daily
    public void runNightlyReconciliation() {
        // BUG: LocalDate.now() without timezone — on EST batch servers this is
        // 5 hours behind UTC, so "yesterday" is actually two days ago after midnight
        LocalDate yesterday = LocalDate.now().minusDays(1);

        Instant from = yesterday.atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant to   = yesterday.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant();

        List<TransactionRepository.TransactionRecord> pending =
            repository.findUnreconciledInRange(from, to);

        log.info("Reconciliation starting for {}: {} transactions to process",
                 yesterday, pending.size());

        // BUG (Issue #5): using double instead of BigDecimal for running total
        double totalDebits  = 0.0;
        double totalCredits = 0.0;

        for (TransactionRepository.TransactionRecord txn : pending) {
            double amount = txn.amount().doubleValue();   // precision loss here

            if ("DEBIT".equals(txn.type())) {
                totalDebits += amount;
            } else if ("CREDIT".equals(txn.type())) {
                totalCredits += amount;
            }

            repository.markReconciled(txn.transactionId());
        }

        // Floating-point comparison — will fail for large volumes
        double net = totalCredits - totalDebits;
        if (Math.abs(net) > 0.001) {
            log.error("RECONCILIATION IMBALANCE for {}: net={} (debits={}, credits={})",
                      yesterday, net, totalDebits, totalCredits);
        } else {
            log.info("Reconciliation complete for {}: {} transactions, net={}",
                     yesterday, pending.size(), net);
        }
    }
}
