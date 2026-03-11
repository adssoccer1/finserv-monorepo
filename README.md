# finserv-monorepo

Backend services for **Meridian Bank** — a mid-size regional bank operating across 12 US states.

## Architecture

This is a Java/Spring Boot monorepo structured as a Maven multi-module project.
Services communicate via internal REST APIs over an mTLS-secured VPC.
Each service is independently deployable as a Docker container on EKS.

```
finserv-monorepo/
├── services/
│   ├── auth-service          # JWT auth, session management, account lockout
│   ├── payments-service      # ACH/wire payment processing, balance checks
│   ├── transaction-service   # Ledger, transaction history, nightly reconciliation
│   └── notification-service  # Email/SMS alerts via SendGrid + Twilio
└── shared/
    ├── utils                 # Shared constants, validation, currency math
    └── middleware            # Auth filter, rate limiter, request logging, error handler
```

## Services

### auth-service
Handles user authentication via JWT tokens. Issues 15-minute access tokens and
30-day refresh tokens. Enforces account lockout after 5 failed login attempts.

**Key classes:**
- `JwtTokenProvider` — token generation and validation
- `UserAuthService` — login, password change, lockout logic
- `SessionManager` — active session tracking (Redis-backed in prod)

### payments-service
Processes ACH payments between internal accounts. Validates amounts, checks balances,
and records transactions atomically (or should — see open issues).

Single-transaction limit: **$50,000 USD**.
Daily aggregate limit enforced by the risk engine (external service, not in this repo).

**Key classes:**
- `PaymentProcessor` — orchestrates the full payment flow
- `PaymentValidator` — request validation rules
- `PaymentRepository` — balance management and payment records

### transaction-service
Maintains the double-entry ledger and provides transaction history APIs.
Runs a nightly reconciliation job at 01:00 UTC.

**Key classes:**
- `LedgerService` — double-entry bookkeeping
- `ReconciliationJob` — nightly batch (scheduled via Spring `@Scheduled`)
- `TransactionRepository` — transaction storage and queries

### notification-service
Sends email and SMS alerts for account activity. Internal-only endpoints
called by other services via service-to-service auth.

**Key classes:**
- `NotificationService` — deduplication and dispatch logic
- `EmailProvider` — SendGrid SMTP wrapper
- `SmsProvider` — Twilio REST wrapper
- `AlertTemplateManager` — message templating

## Local Development

### Prerequisites
- Java 17+
- Maven 3.9+
- Docker (for local Postgres + Redis)

### Build
```bash
mvn clean install -DskipTests
```

### Run a service locally
```bash
cd services/auth-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Environment variables
Each service reads configuration from `application-local.yml`.
Copy `.env.example` to `.env` and fill in the required values.

| Variable                  | Description                        | Required |
|---------------------------|------------------------------------|----------|
| `FINSERV_JWT_SECRET`      | Base64-encoded 256-bit HMAC secret | Yes      |
| `FINSERV_DB_URL`          | PostgreSQL JDBC URL                | Yes      |
| `FINSERV_REDIS_URL`       | Redis connection string            | Yes      |
| `FINSERV_TWILIO_ACCOUNT_SID` | Twilio account SID              | No       |
| `FINSERV_TWILIO_AUTH_TOKEN`  | Twilio auth token               | No       |
| `FINSERV_EMAIL_FROM`      | Sender email address               | No       |

## Testing
```bash
mvn test                    # unit tests
mvn verify -P integration   # integration tests (requires Docker)
```

## Deployment
Services are deployed via GitHub Actions → ECR → EKS.
See `.github/workflows/` for CI/CD pipeline configuration.

Production environments:
- **prod-us-east-1** — primary (Virginia)
- **prod-us-west-2** — DR (Oregon)

## On-call & Runbooks
PagerDuty team: `finserv-backend-oncall`
Runbooks: [Confluence: FinServ Backend Runbooks](https://meridianbank.atlassian.net/wiki)

---
*Maintained by the Platform Engineering team. Questions? Ping #backend-platform in Slack.*
