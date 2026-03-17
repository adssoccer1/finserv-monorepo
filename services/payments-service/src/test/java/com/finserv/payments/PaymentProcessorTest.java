package com.finserv.payments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #18 fix: idempotency key enforcement in PaymentProcessor.
 */
class PaymentProcessorTest {

    private PaymentProcessor processor;
    private PaymentRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PaymentRepository();
        PaymentValidator validator = new PaymentValidator();
        processor = new PaymentProcessor(validator, repository);
    }

    // --- Happy path: first request with idempotency key succeeds ---
    @Test
    void process_withIdempotencyKey_shouldSucceedOnFirstCall() {
        PaymentRequest request = new PaymentRequest(
            "100000001", "200000001",
            new BigDecimal("100.00"), "USD",
            "Test payment", "idem-key-001");

        PaymentProcessor.PaymentResult result = processor.process(request);

        assertTrue(result.success());
        assertNotNull(result.paymentId());
        assertEquals(new BigDecimal("100.00"), result.amount());
        assertEquals("USD", result.currency());
    }

    // --- Happy path: duplicate request with same key returns cached result ---
    @Test
    void process_withDuplicateIdempotencyKey_shouldReturnCachedResult() {
        PaymentRequest request = new PaymentRequest(
            "100000001", "200000001",
            new BigDecimal("100.00"), "USD",
            "Test payment", "idem-key-002");

        PaymentProcessor.PaymentResult first = processor.process(request);
        assertTrue(first.success());

        // Record balance after first payment
        long balanceAfterFirst = repository.getBalanceCents("100000001");

        // Same idempotency key — should return cached result without re-processing
        PaymentProcessor.PaymentResult second = processor.process(request);
        assertTrue(second.success());
        assertEquals(first.paymentId(), second.paymentId());
        assertEquals(first.amount(), second.amount());

        // Balance should NOT have changed (no double-debit)
        assertEquals(balanceAfterFirst, repository.getBalanceCents("100000001"));
    }

    // --- Edge case: null idempotency key should process normally ---
    @Test
    void process_withNullIdempotencyKey_shouldProcessNormally() {
        PaymentRequest request = new PaymentRequest(
            "100000001", "200000001",
            new BigDecimal("50.00"), "USD",
            "No idem key", null);

        PaymentProcessor.PaymentResult result = processor.process(request);
        assertTrue(result.success());
    }

    // --- Edge case: blank idempotency key should process normally ---
    @Test
    void process_withBlankIdempotencyKey_shouldProcessNormally() {
        PaymentRequest request = new PaymentRequest(
            "100000001", "200000001",
            new BigDecimal("50.00"), "USD",
            "Blank idem key", "  ");

        PaymentProcessor.PaymentResult result = processor.process(request);
        assertTrue(result.success());
    }

    // --- Different idempotency keys should process independently ---
    @Test
    void process_withDifferentIdempotencyKeys_shouldProcessIndependently() {
        PaymentRequest request1 = new PaymentRequest(
            "100000001", "200000001",
            new BigDecimal("100.00"), "USD",
            "Payment 1", "idem-key-A");

        PaymentRequest request2 = new PaymentRequest(
            "100000001", "200000001",
            new BigDecimal("200.00"), "USD",
            "Payment 2", "idem-key-B");

        PaymentProcessor.PaymentResult result1 = processor.process(request1);
        PaymentProcessor.PaymentResult result2 = processor.process(request2);

        assertTrue(result1.success());
        assertTrue(result2.success());
        assertNotEquals(result1.paymentId(), result2.paymentId());
    }
}
