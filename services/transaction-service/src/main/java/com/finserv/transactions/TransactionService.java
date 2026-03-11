package com.finserv.transactions;

import com.finserv.utils.ErrorCodes;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Application-layer service for transaction operations.
 * Coordinates between TransactionRepository and LedgerService.
 */
@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final LedgerService         ledgerService;

    public TransactionService(TransactionRepository repository, LedgerService ledgerService) {
        this.repository    = repository;
        this.ledgerService = ledgerService;
    }

    /**
     * Records a payment event as a transaction (called by payments-service via internal API).
     * Creates both a debit and credit ledger entry.
     */
    public TransactionRepository.TransactionRecord recordPaymentTransaction(
            String paymentId,
            String sourceAccountId,
            String destinationAccountId,
            BigDecimal amount,
            String currency,
            String description) {

        // This triggers the double-entry write (see LedgerService BUG)
        ledgerService.recordPayment(paymentId, sourceAccountId, destinationAccountId,
                                    amount, currency);

        // Return the debit-side record as the "primary" transaction
        String txnId = repository.generateTransactionId();
        return repository.save(new TransactionRepository.TransactionRecord(
            txnId, sourceAccountId, "DEBIT",
            amount, currency, "COMPLETED",
            paymentId, description,
            Instant.now(), false));
    }

    public TransactionRepository.TransactionRecord getTransaction(String transactionId) {
        return repository.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException(
                ErrorCodes.TXN_NOT_FOUND + ": " + transactionId));
    }

    /**
     * Returns paginated transaction history for an account.
     * Default page size is 50; caller can override.
     *
     * BUG (Issue #16): If pageSize <= 0, the repository returns all records (no limit).
     * The API currently does not enforce a max page size.
     */
    public List<TransactionRepository.TransactionRecord> getHistory(
            String accountId, int page, int pageSize) {
        int offset = page * pageSize;
        return repository.findByAccountId(accountId, offset, pageSize);
    }

    public BigDecimal getLedgerBalance(String accountId) {
        return ledgerService.getLedgerBalance(accountId);
    }
}
