# Demo Notes — finserv-monorepo Issue Guide

Quick reference for the Loom demo showing autonomous issue triage and fixes.

---

## Issue Overview

| # | Title (short) | Size | Autonomous? | Why |
|---|---------------|------|-------------|-----|
| 1  | Zero-dollar payments accepted | S | ✅ Yes | Single-line fix in `ValidationUtils.java` |
| 2  | Access token accepted as refresh token | S | ✅ Yes | Add type-claim check in `JwtTokenProvider.java` |
| 3  | Transaction dates off by timezone | S | ✅ Yes | Change `ZoneId.systemDefault()` → `ZoneId.of("UTC")` |
| 4  | Duplicate notifications on retry | S | ✅ Yes | Fix dedup key to use event ID, not timestamp |
| 5  | Floating-point errors in reconciliation | S | ✅ Yes | Replace `double` with `BigDecimal` in `ReconciliationJob` |
| 6  | Sessions survive password change | S | ✅ Yes | Add `invalidateAllSessions()` call in `UserAuthService` |
| 7  | Account number regex too permissive | S | ✅ Yes | Add Luhn/checksum validation in `ValidationUtils` |
| 8  | Rate limiter allows N+1 requests | S | ✅ Yes | Fix `>` vs `>=` off-by-one in `RateLimitMiddleware` |
| 9  | Sensitive fields logged unmasked | M | ✅ Yes | Add field masking to `LoggingMiddleware` |
| 10 | Race condition in payment balance check | M | ⚠️ Needs guidance | Requires DB locking strategy decision |
| 11 | Ledger imbalance on crash between debit/credit | M | ⚠️ Needs guidance | Requires `@Transactional` + saga discussion |
| 12 | JWT algorithm confusion (alg=none) | M | ✅ Yes | Enforce algorithm in `AuthMiddleware` parser config |
| 13 | Email failures silently swallowed | M | ✅ Yes | Check return value; add retry/alerting logic |
| 14 | Reconciliation skips midnight-UTC transactions | M | ✅ Yes | Fix `LocalDate.now()` to use explicit `ZoneId.of("UTC")` |
| 15 | Payments are slow sometimes | M/L | ❌ Ambiguous | No reproduction steps, needs profiling data |
| 16 | Transaction history not showing all records | M | ❌ Ambiguous | Unclear if pagination bug or data issue |
| 17 | Users randomly getting logged out | M | ❌ Ambiguous | Could be session TTL, JWT, load balancer — needs investigation |
| 18 | Feature: Idempotency keys for payments | M | ✅ Yes | Well-scoped, `PaymentRequest` already has the field |
| 19 | Feature: Auth event audit trail | M | ⚠️ Needs guidance | Needs schema/storage decisions |
| 20 | Stack traces exposed in API error responses | S | ✅ Yes | Remove `exceptionType` + sanitize `message` in `ErrorHandler` |

---

## Detailed Issue Notes

### Issue #1 — Zero-dollar payments accepted (S) ✅
**File:** `shared/utils/src/main/java/com/finserv/utils/ValidationUtils.java:52`
**Bug:** `amount.compareTo(BigDecimal.ZERO) >= 0` should be `> 0`
**Why autonomous:** Single character change, obvious intent, no downstream risk.
**Good demo:** Shows Devin reading the bug in the issue, finding the exact line, making the fix, and verifying the logic.

### Issue #2 — Access token accepted as refresh token (S) ✅
**File:** `services/auth-service/.../JwtTokenProvider.java:refreshAccessToken()`
**Bug:** Missing `claims.get("type").equals("refresh")` check.
**Why autonomous:** Self-contained, clear fix, no API changes needed.

### Issue #3 — Transaction timestamps off by one day (S) ✅
**File:** `shared/utils/src/main/java/com/finserv/utils/DateUtils.java:toLocalDate()`
**Bug:** `ZoneId.systemDefault()` should be `ZoneId.of("UTC")`
**Why autonomous:** One-line fix, crystal-clear root cause in the issue.

### Issue #4 — Duplicate notifications on retry (S) ✅
**File:** `services/notification-service/.../NotificationService.java`
**Bug:** Dedup key uses `Instant.now().getEpochSecond() / 60` (minute bucket) instead of event ID.
**Why autonomous:** Clear description, fix is contained to one method.

### Issue #5 — Penny discrepancies in reconciliation reports (S) ✅
**File:** `services/transaction-service/.../ReconciliationJob.java`
**Bug:** `double totalDebits = 0.0` — should be `BigDecimal`.
**Why autonomous:** Classic Java money bug, fix is mechanical.

### Issue #6 — Sessions not invalidated after password change (S) ✅
**File:** `services/auth-service/.../UserAuthService.java:changePassword()`
**Bug:** Missing `sessionManager.invalidateAllSessions(userId)` call after password update.
**Why autonomous:** One line addition, clear security implication stated in issue.

### Issue #7 — Account number validation too permissive (S) ✅
**File:** `shared/utils/src/main/java/com/finserv/utils/ValidationUtils.java`
**Bug:** Regex `^[0-9]{8,12}$` doesn't validate routing checksum.
**Why autonomous:** The issue is scoped to adding a checksum; Devin can implement Luhn or ABA checksum.

### Issue #8 — Rate limiter off-by-one (S) ✅
**File:** `shared/middleware/.../RateLimitMiddleware.java:WindowCounter.tryAcquire()`
**Bug:** `count.incrementAndGet() > MAX_REQUESTS_PER_WINDOW` should be `>=`
**Why autonomous:** Canonical off-by-one, immediately visible in the code.

### Issue #9 — Sensitive data logged in plain text (M) ✅
**File:** `shared/middleware/.../LoggingMiddleware.java`
**Bug:** Raw request body logged — includes card numbers, account numbers.
**Why autonomous:** Well-described, fix pattern (field masking) is well-known. May touch 2 files if a masking utility is added.

### Issue #10 — Race condition: concurrent payments overdraft accounts (M) ⚠️
**Files:** `PaymentProcessor.java`, `PaymentRepository.java`
**Bug:** Balance check and debit are not atomic (TOCTOU).
**Why needs guidance:** Fix requires choosing between pessimistic locking, optimistic locking, or atomic CAS — a design decision Devin should flag.
**Good demo moment:** Devin identifies the race, proposes options, asks for direction.

### Issue #11 — Ledger imbalance on partial failure (M) ⚠️
**Files:** `LedgerService.java`, `TransactionRepository.java`
**Bug:** Debit and credit writes aren't wrapped in a transaction boundary.
**Why needs guidance:** Fix is `@Transactional` (trivial) but saga/outbox pattern is the right long-term answer.

### Issue #12 — JWT algorithm confusion vulnerability (M) ✅
**File:** `shared/middleware/.../AuthMiddleware.java`
**Bug:** `parserBuilder()` without explicit algorithm enforcement.
**Why autonomous:** Well-known CVE class, fix is adding `.setAllowedAlgorithms()` or equivalent.

### Issue #13 — Email delivery failures silently ignored (M) ✅
**Files:** `EmailProvider.java`, `NotificationService.java`
**Bug:** `emailProvider.send()` return value ignored; failures lost.
**Why autonomous:** Fix is to check return value and log/throw; straightforward 2-file change.

### Issue #14 — Reconciliation skips midnight-UTC transactions (M) ✅
**File:** `ReconciliationJob.java`
**Bug:** `LocalDate.now()` without timezone on batch servers with different TZ than prod.
**Why autonomous:** Same class of bug as Issue #3, clear fix: `LocalDate.now(ZoneId.of("UTC"))`.

### Issue #15 — Payments slow "sometimes" (M/L) ❌
**No file reference, no reproduction steps.**
**Why ambiguous:** Could be DB query plan, rate limiter overhead, network, memory pressure. Devin should ask for metrics, APM traces, or a way to reproduce.
**Good demo moment:** Devin responds asking for profiling data instead of guessing.

### Issue #16 — Transaction history missing records (M) ❌
**File:** `TransactionRepository.java` (maybe)
**Why ambiguous:** Could be the `limit=0` bug (returns nothing vs. returns all), a pagination off-by-one, or a data issue. Unclear from description.
**Good demo moment:** Devin asks clarifying questions about page size, account ID, date range.

### Issue #17 — Users randomly logged out (M) ❌
**Why ambiguous:** Root cause unknown — JWT expiry mismatch, Redis TTL mismatch, load balancer session stickiness, or clock skew between pods.
**Good demo moment:** Devin asks for logs, error codes, and whether it correlates with deployments.

### Issue #18 — Feature: Idempotency keys for payments (M) ✅
**File:** `PaymentProcessor.java`, `PaymentRequest.java`
**Scope:** `idempotencyKey` field already exists in `PaymentRequest`; needs enforcement logic.
**Why autonomous:** Well-scoped, field already stubbed out with a TODO comment.

### Issue #19 — Feature: Auth event audit trail (M) ⚠️
**File:** `UserAuthService.java`, potentially new `AuditService.java`
**Scope:** Log all login/logout/password-change events to a durable audit table.
**Why needs guidance:** Needs decisions on storage schema (separate DB table? S3? event stream?).

### Issue #20 — Stack traces exposed in error responses (S) ✅ **[SECURITY]**
**File:** `shared/middleware/.../ErrorHandler.java`
**Bug:** `e.getClass().getName()` and `e.getMessage()` returned directly in 500 responses.
**Why autonomous:** OWASP A05, clear fix: return a generic message; log details server-side only.

---

## Recommended Demo Script

### Segment 1 — Small autonomous fixes (10 min)
Show Issues #1, #8, #20 — fast, visible, satisfying. Good for showing the edit → verify loop.

### Segment 2 — Medium investigation (10 min)
Show Issue #9 (logging) or #14 (reconciliation timezone) — demonstrates multi-file reasoning.

### Segment 3 — Ambiguous issue (5 min)
Show Issue #15 or #17 — demonstrates Devin asking smart clarifying questions instead of guessing.

### Segment 4 — Security issue (5 min)
Show Issue #20 or #12 — demonstrates security-aware reasoning.
