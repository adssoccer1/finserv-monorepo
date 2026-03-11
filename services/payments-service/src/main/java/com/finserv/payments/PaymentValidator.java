package com.finserv.payments;

import com.finserv.utils.ValidationUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates payment requests before processing.
 * All fields are mandatory unless annotated with (optional).
 */
@Component
public class PaymentValidator {

    /**
     * Validates a payment request. Returns a list of validation errors;
     * empty list means the request is valid.
     *
     * BUG (Issue #1): isValidAmount() in ValidationUtils accepts zero (>= 0 instead of > 0).
     * This means a payment of $0.00 passes validation and gets processed — creating a
     * zero-dollar transaction record and triggering a notification.
     * Reported by QA team 2024-03-12. Reproducible in staging and production.
     */
    public List<String> validate(PaymentRequest request) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add("Request must not be null");
            return errors;
        }

        if (!ValidationUtils.isValidAccountNumber(request.sourceAccountId())) {
            errors.add("Invalid source account number: " + request.sourceAccountId());
        }

        if (!ValidationUtils.isValidAccountNumber(request.destinationAccountId())) {
            errors.add("Invalid destination account number: " + request.destinationAccountId());
        }

        if (request.sourceAccountId() != null &&
            request.sourceAccountId().equals(request.destinationAccountId())) {
            errors.add("Source and destination accounts must be different");
        }

        // BUG: delegates to ValidationUtils.isValidAmount() which allows zero
        if (!ValidationUtils.isValidAmount(request.amount())) {
            errors.add("Invalid amount: must be a positive value with at most 2 decimal places");
        }

        if (request.currency() == null || request.currency().isBlank()) {
            errors.add("Currency is required");
        } else if (!List.of("USD", "EUR", "GBP", "CAD").contains(request.currency())) {
            errors.add("Unsupported currency: " + request.currency());
        }

        if (request.memo() != null && request.memo().length() > 140) {
            errors.add("Memo must not exceed 140 characters");
        }

        return errors;
    }

    /**
     * Validates that a payment amount does not exceed single-transaction limits.
     * Individual limit: $50,000 USD. Daily aggregate: enforced by PaymentProcessor.
     */
    public boolean exceedsSingleTransactionLimit(BigDecimal amount) {
        return amount.compareTo(new BigDecimal("50000.00")) > 0;
    }
}
