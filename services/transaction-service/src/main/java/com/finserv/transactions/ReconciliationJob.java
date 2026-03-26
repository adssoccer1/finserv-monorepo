package com.finserv.transactions;

import com.finserv.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

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
        LocalDate yesterday = LocalDate.now(ZoneId.of("UTC")).minusDays(1);

        Instant from = yesterday.atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant to   = yesterday.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant();

        List<TransactionRepository.TransactionRecord> pending =
            repository.findUnreconciledInRange(from, to);

        log.info("Reconciliation starting for {}: {} transactions to process",
                 yesterday, pending.size());

        BigDecimal totalDebits  = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (TransactionRepository.TransactionRecord txn : pending) {
            if ("DEBIT".equals(txn.type())) {
                totalDebits = totalDebits.add(txn.amount());
            } else if ("CREDIT".equals(txn.type())) {
                totalCredits = totalCredits.add(txn.amount());
            }

            repository.markReconciled(txn.transactionId());
        }

        BigDecimal net = totalCredits.subtract(totalDebits);
        if (net.compareTo(BigDecimal.ZERO) != 0) {
            log.error("RECONCILIATION IMBALANCE for {}: net={} (debits={}, credits={})",
                      yesterday, net, totalDebits, totalCredits);
        } else {
            log.info("Reconciliation complete for {}: {} transactions, net={}",
                     yesterday, pending.size(), net);
        }
    }
}
