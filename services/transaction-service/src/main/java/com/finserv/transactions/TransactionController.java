package com.finserv.transactions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API for transaction history and ledger queries.
 * Base path: /api/v1/transactions
 */
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<Map<String, Object>> getTransaction(
            @PathVariable String transactionId,
            @RequestAttribute("userId") String requesterId) {
        try {
            TransactionRepository.TransactionRecord txn =
                transactionService.getTransaction(transactionId);
            return ResponseEntity.ok(toMap(txn));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of(
                "error", "TXN_001",
                "message", "Transaction not found: " + transactionId));
        }
    }

    /**
     * Returns transaction history for an account.
     *
     * Query params:
     *   page     - zero-based page index (default 0)
     *   pageSize - records per page (default 50, no enforced max)
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestAttribute("userId") String requesterId) {

        List<Map<String, Object>> results =
            transactionService.getHistory(accountId, page, pageSize)
                .stream()
                .map(this::toMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    @GetMapping("/account/{accountId}/balance")
    public ResponseEntity<Map<String, Object>> getLedgerBalance(
            @PathVariable String accountId,
            @RequestAttribute("userId") String requesterId) {
        return ResponseEntity.ok(Map.of(
            "accountId", accountId,
            "ledgerBalance", transactionService.getLedgerBalance(accountId)
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "transaction-service"));
    }

    private Map<String, Object> toMap(TransactionRepository.TransactionRecord t) {
        return Map.of(
            "transactionId",      t.transactionId(),
            "accountId",          t.accountId(),
            "type",               t.type(),
            "amount",             t.amount(),
            "currency",           t.currency(),
            "status",             t.status(),
            "description",        t.description() != null ? t.description() : "",
            "createdAt",          t.createdAt().toString(),
            "reconciled",         t.reconciled()
        );
    }
}
