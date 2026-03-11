package com.finserv.transactions;

import com.finserv.utils.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Double-entry ledger service. Every payment creates both a DEBIT on the source
 * account and a CREDIT on the destination account.
 *
 * Invariant: sum of all ledger entries for a closed period must equal zero.
 *
 * BUG (Issue #11 - medium): recordPayment() writes the debit and credit as two
 * separate operations with no transaction boundary. If the process crashes or
 * an exception is thrown between the two writes, the ledger becomes imbalanced:
 * the debit is recorded but the corresponding credit is missing.
 *
 * Fix: wrap both writes in a @Transactional method (JPA) or use a saga pattern.
 */
@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);

    private final TransactionRepository transactionRepository;
    // ledger balance cache: accountId -> net balance
    private final Map<String, BigDecimal> ledgerBalances = new ConcurrentHashMap<>();

    public LedgerService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Records a completed payment as a double-entry ledger event.
     * Debit on source, credit on destination.
     *
     * BUG: these two writes are not atomic — if an exception occurs after
     * the debit write but before the credit write, the ledger is imbalanced.
     */
    public void recordPayment(String paymentId, String sourceAccountId,
                              String destinationAccountId, BigDecimal amount,
                              String currency) {

        // Write 1: debit source
        String debitTxnId = transactionRepository.generateTransactionId();
        transactionRepository.save(new TransactionRepository.TransactionRecord(
            debitTxnId, sourceAccountId, "DEBIT",
            amount, currency, "COMPLETED",
            paymentId, "Payment debit",
            Instant.now(), false));

        ledgerBalances.merge(sourceAccountId, amount.negate(), BigDecimal::add);

        // Simulate intermittent failure (would be a DB failure in prod)
        // BUG: if exception thrown here, credit is never written
        if (shouldSimulateFailure()) {
            throw new RuntimeException("Transient error writing credit entry — ledger imbalanced!");
        }

        // Write 2: credit destination
        String creditTxnId = transactionRepository.generateTransactionId();
        transactionRepository.save(new TransactionRepository.TransactionRecord(
            creditTxnId, destinationAccountId, "CREDIT",
            amount, currency, "COMPLETED",
            paymentId, "Payment credit",
            Instant.now(), false));

        ledgerBalances.merge(destinationAccountId, amount, BigDecimal::add);
        log.info("Ledger updated for payment {}: debit {} credited to {}",
                 paymentId, sourceAccountId, destinationAccountId);
    }

    public BigDecimal getLedgerBalance(String accountId) {
        return ledgerBalances.getOrDefault(accountId, BigDecimal.ZERO);
    }

    /**
     * Verifies that all ledger entries for a set of accounts sum to zero (double-entry check).
     */
    public boolean isBalanced(Iterable<String> accountIds) {
        BigDecimal total = BigDecimal.ZERO;
        for (String id : accountIds) {
            total = total.add(getLedgerBalance(id));
        }
        return total.compareTo(BigDecimal.ZERO) == 0;
    }

    private boolean shouldSimulateFailure() {
        // In production this is never true; used only in chaos testing
        return Boolean.getBoolean("finserv.ledger.chaos.enabled");
    }
}
