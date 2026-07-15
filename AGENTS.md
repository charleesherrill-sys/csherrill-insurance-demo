# AGENTS.md — Guide for AI Software Engineering Agents

This document describes the **Aegis Claims Platform** for AI agents asked to explore,
investigate, fix, or extend the codebase.

## What this repo is

A legacy insurance/benefits monolith (claims, policy, billing) built on **Java 8 + Spring Boot
2.3.x**, with **hand-written JDBC** over **PostgreSQL** and **Thymeleaf** server-rendered pages.
It is a representative extract of a much larger legacy estate. It intentionally contains security
vulnerabilities, performance problems, and duplicated/dead code. Those are documented in
[`REVIEW.md`](REVIEW.md) and must not be "cleaned up" unless a task explicitly asks for it.

## Build, run, test, audit

| Task | Command |
|------|---------|
| Compile + run tests + build jar | `mvn clean package` |
| Run the full stack (app + Postgres) | `docker-compose up --build` |
| Dependency / security audit | `mvn -Paudit org.owasp:dependency-check-maven:check` |

Notes:
- There is **no separate lint step**; treat a clean `mvn package` (compile + tests) as the gate.
- The unit tests under `src/test/java` do **not** require a database. The application does; use
  `docker-compose up` to run it.
- The test suite is sparse and **partly flaky on purpose**. `com.aegis.batch.ReconciliationTimingTest`
  seeds its RNG from the wall clock and fails intermittently. This is intentional (see REVIEW.md);
  do not stabilize it unless that is the task.
- CI (`.github/workflows/ci.yml`) runs the build/tests against a Postgres service and runs the
  dependency audit as a separate job. The dependency audit is **expected to fail** because of the
  seeded vulnerable dependencies.

## Architecture

Requests flow: Thymeleaf page -> `*.web.*Controller` -> `*.service.*Service` ->
`*.repository.*Repository` (raw JDBC via `com.aegis.common.db.Database`) -> Postgres.

Authentication is a cookie session (`AEGIS_SESSION`) resolved by
`com.aegis.auth.web.AuthInterceptor`, which sets the current user on the request. The interceptor
enforces **authentication only**. Per-record **authorization** (ownership checks) is left to
individual controllers/services.

### Packages (`com.aegis.*`)

```
com.aegis
├── AegisApplication            # Spring Boot entry point (@EnableScheduling)
├── common
│   ├── db/Database             # DataSource wrapper used by all repositories
│   ├── config/AppConfig        # config holder (hardcoded fallback secrets)
│   ├── audit/AuditService      # writes audit_log rows (claim views, etc.)
│   └── web/WebConfig,          # interceptor registration
│           DashboardController # post-login dashboard
├── auth                        # LoginController, AuthInterceptor, AuthService,
│                               # SessionManager, PasswordHasher (MD5), UserRepository
├── policy                      # PolicyController, PolicyService, PolicyRepository
├── claims                      # THE CORE SUBSYSTEM
│   ├── web/ClaimsController         # GET /claims (list)
│   ├── web/ClaimDetailController    # GET /claims/{id}  <-- flagship IDOR
│   ├── web/ClaimIntakeController    # new-claim form, submit, adjudicate
│   ├── service/ClaimService         # list/detail reads (N+1)
│   ├── service/ClaimIntakeService   # submit + validate
│   ├── service/AdjudicationService  # adjudicate + trigger payment
│   └── repository/ClaimRepository   # raw JDBC (SQLi in searchByStatus)
├── billing                     # BillingController, BillingService, PaymentService,
│                               # BillingRepository (SQLi in searchInvoices, N+1)
├── document                    # DocumentController/Service (path traversal), DocumentSeeder
├── admin                       # AdminController (unauthenticated), AdminService
├── reporting                   # ReportingController, ReportingService, ReportingRepository
├── batch                       # ReconciliationJob (@Scheduled), ReconciliationService
└── integration                 # EligibilityService, FraudCheckService,
                                # PaymentGatewayClient, NotificationService (all mocked, blocking)
```

### The core flow: claim adjudication

`submit -> validate -> adjudicate -> trigger payment`

1. `ClaimIntakeService.submit` inserts a claim in `SUBMITTED`.
2. `ClaimIntakeService.validate` checks the policy is active and the member is eligible
   (`integration.EligibilityService`), moving the claim to `VALIDATED` or `DENIED`.
3. `AdjudicationService.adjudicate` runs a fraud score (`integration.FraudCheckService`), computes
   the approved amount from allowed line amounts, and persists the decision.
4. `AdjudicationService.triggerPayment` disburses via `billing.PaymentService`, which calls the
   mocked `integration.PaymentGatewayClient` and records a payment.

## Key endpoints

| Method | Path | Handler |
|--------|------|---------|
| GET | `/login`, POST `/login` | `auth.web.LoginController` |
| GET | `/dashboard` | `common.web.DashboardController` |
| GET | `/claims` | `claims.web.ClaimsController` |
| GET | `/claims/{id}` | `claims.web.ClaimDetailController.getClaim` |
| POST | `/claims`, `/claims/{id}/adjudicate` | `claims.web.ClaimIntakeController` |
| GET | `/billing` | `billing.web.BillingController` |
| GET | `/documents/download` | `document.web.DocumentController` |
| GET | `/admin`, `/admin/users`, POST `/admin/reconciliation/run` | `admin.web.AdminController` |

## Data

Schema is `db/schema.sql`; seed data is `db/seed.sql`. Both are applied automatically by the
Postgres container. Tables: `users`, `policies`, `claims`, `claim_lines`, `invoices`, `payments`,
`documents`, `audit_log`, `reconciliation_runs`.

## Conventions

- Raw JDBC only in repositories; there is no ORM. Follow the existing try-with-resources pattern.
- Controllers read the current user with `com.aegis.auth.web.CurrentUser.from(request)`.
- Money is stored and passed around as integer cents; formatting helpers live on the models.
- **Do not remove the intentional patterns documented in `REVIEW.md`** unless the task explicitly
  targets one of them. When a task does target one, fix that issue and add a regression test.

## Intentional patterns (summary)

Security: IDOR on `GET /claims/{id}` (CWE-639, flagship), SQL injection (CWE-89), missing auth on
admin endpoints (CWE-306), hardcoded secrets (CWE-798), MD5 password hashing (CWE-327/916), path
traversal in document download (CWE-22), and pinned vulnerable dependencies. Performance: N+1
queries on the claims-list and billing paths, no caching, synchronous blocking integration calls,
duplicated business logic, and dead code after error paths. Full details and rationale in
[`REVIEW.md`](REVIEW.md).
