package com.finserv.payments;

import com.finserv.utils.CurrencyUtils;
import com.finserv.utils.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Core payment processing engine.
 *
 * Flow: validate -> check balance -> debit source -> credit destination -> record
 *
 * BUG (Issue #10 - medium): The balance-check and debit are not atomic.
 * getBalanceCents() is called first, then debitAccount() is called separately.
 * Under concurrent load two threads can both pass the balance check before
 * either debit completes, resulting in overdrafts.
 *
 * Fix requires either pessimistic DB row locking (SELECT FOR UPDATE) or
 * optimistic locking with a version column.
 */
@Service
public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);

    private final PaymentValidator  validator;
    private final PaymentRepository repository;

    public PaymentProcessor(PaymentValidator validator, PaymentRepository repository) {
        this.validator  = validator;
        this.repository = repository;
    }

    public PaymentResult process(PaymentRequest request) {
        // Step 1: validate
        List<String> errors = validator.validate(request);
        if (!errors.isEmpty()) {
            return PaymentResult.failure(ErrorCodes.PAYMENT_INVALID_AMOUNT, errors.toString());
        }

        if (validator.exceedsSingleTransactionLimit(request.amount())) {
            return PaymentResult.failure(ErrorCodes.PAYMENT_INVALID_AMOUNT,
                "Amount exceeds single-transaction limit of $50,000");
        }

        long amountCents = CurrencyUtils.toCents(request.amount());

        // Step 2: check balance (non-atomic — see class-level BUG note)
        long balance = repository.getBalanceCents(request.sourceAccountId());
        if (balance < 0) {
            return PaymentResult.failure(ErrorCodes.PAYMENT_ACCOUNT_NOT_FOUND,
                "Source account not found: " + request.sourceAccountId());
        }
        if (balance < amountCents) {
            return PaymentResult.failure(ErrorCodes.PAYMENT_INSUFFICIENT_FUNDS,
                String.format("Insufficient funds. Available: %s, Requested: %s",
                    CurrencyUtils.formatUSD(CurrencyUtils.fromCents(balance)),
                    CurrencyUtils.formatUSD(request.amount())));
        }

        // Step 3: debit source (race window between step 2 and here)
        String paymentId = repository.generatePaymentId();
        PaymentRepository.PaymentRecord pending = new PaymentRepository.PaymentRecord(
            paymentId, request.sourceAccountId(), request.destinationAccountId(),
            request.amount(), request.currency(), "PENDING",
            request.memo(), Instant.now(), Instant.now());
        repository.save(pending);

        boolean debited = repository.debitAccount(request.sourceAccountId(), amountCents);
        if (!debited) {
            repository.save(toPending(pending, "FAILED"));
            return PaymentResult.failure(ErrorCodes.PAYMENT_INSUFFICIENT_FUNDS,
                "Debit failed — possible concurrent modification");
        }

        // Step 4: credit destination
        repository.creditAccount(request.destinationAccountId(), amountCents);

        PaymentRepository.PaymentRecord completed = toPending(pending, "COMPLETED");
        repository.save(completed);

        log.info("Payment {} processed: {} {} from {} to {}", paymentId,
            request.amount(), request.currency(),
            request.sourceAccountId(), request.destinationAccountId());

        return PaymentResult.success(paymentId, request.amount(), request.currency());
    }

    private PaymentRepository.PaymentRecord toPending(
            PaymentRepository.PaymentRecord r, String status) {
        return new PaymentRepository.PaymentRecord(
            r.paymentId(), r.sourceAccountId(), r.destinationAccountId(),
            r.amount(), r.currency(), status, r.memo(), r.createdAt(), Instant.now());
    }

    public record PaymentResult(boolean success, String paymentId,
                                BigDecimal amount, String currency,
                                String errorCode, String errorMessage) {
        static PaymentResult success(String id, BigDecimal amt, String ccy) {
            return new PaymentResult(true, id, amt, ccy, null, null);
        }
        static PaymentResult failure(String code, String msg) {
            return new PaymentResult(false, null, null, null, code, msg);
        }
    }
}
