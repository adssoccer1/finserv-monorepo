package com.finserv.payments;

import java.math.BigDecimal;

/**
 * Immutable value object representing an inbound payment request.
 *
 * All monetary amounts are in the specified currency with up to 2 decimal places.
 * The idempotencyKey field is optional — if provided, duplicate requests with the
 * same key within 24h will return the original result without re-processing.
 *
 * NOTE: idempotencyKey enforcement is not yet implemented.
 * See feature request: Issue #18
 */
public record PaymentRequest(
    String     sourceAccountId,
    String     destinationAccountId,
    BigDecimal amount,
    String     currency,
    String     memo,
    String     idempotencyKey   // optional; not yet enforced — see Issue #18
) {}
