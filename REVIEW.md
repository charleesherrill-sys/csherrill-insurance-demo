# Review Guidelines

This is a **demonstration codebase**. The weaknesses below are seeded on purpose so the repo can
be used to explore security review, performance analysis, and legacy modernization. Every item
here is intentional and should **not** be flagged as an accidental bug or "fixed" during a general
cleanup. When a task explicitly targets one of these, fix that specific issue and add a regression
test; leave the rest in place.

## Intentional security weaknesses

| # | Issue | CWE | Where |
|---|-------|-----|-------|
| 1 | **Broken access control / IDOR (flagship)** — the claim-detail endpoint loads a claim by id and renders it without verifying ownership, so any authenticated user can read any other member's claim by changing the id. | CWE-639 | `claims.web.ClaimDetailController.getClaim` (`GET /claims/{id}`) |
| 2 | **SQL injection** — the claims and billing status filters build SQL by string concatenation. | CWE-89 | `claims.repository.ClaimRepository.searchByStatus`, `billing.repository.BillingRepository.searchInvoices` |
| 3 | **Missing authentication for critical function** — the entire `/admin/**` area is not behind the auth interceptor; `/admin/users` dumps users + password hashes and `/admin/reconciliation/run` triggers a financial batch. | CWE-306 | `admin.web.AdminController`, `common.web.WebConfig` (admin paths deliberately not intercepted) |
| 4 | **Hardcoded credentials/secrets (REMEDIATED)** — the integration API key, fraud shared secret, admin bootstrap password, and DB password are no longer stored in source. They are sourced from environment variables with no in-source default (fail-closed). | CWE-798 | `src/main/resources/application.properties`, `common.config.AppConfig`, `.env.example` |
| 5 | **Weak password hashing** — passwords are stored as unsalted MD5. | CWE-327, CWE-916 | `auth.service.PasswordHasher` |
| 6 | **Path traversal** — the document download endpoint joins a caller-supplied filename to the storage root with no containment check. | CWE-22 | `document.service.DocumentService.readDocument`, `document.web.DocumentController` |
| 7 | **Known-vulnerable dependencies** — `log4j-core` / `log4j-api` 2.14.1 (Log4Shell, CVE-2021-44228) and `commons-collections` 3.2.1 (CVE-2015-7501) are pinned so the CI dependency audit has something concrete to flag. | — | `pom.xml`, `integration.NotificationService`, `batch.ReconciliationService` |

The empty `owasp-suppressions.xml` is intentional: the audit is meant to fail loudly. Do not add
suppressions to make it pass.

## Intentional performance / cost problems

These are the "losing money" angle. Do not optimize them away during general review.

- **N+1 queries (flagship performance issue)** — the claims-list and billing paths run one query
  for the list and then two more per row (service lines/payments + policy).
  See `claims.service.ClaimService.getClaimsForMember` and
  `billing.service.BillingService.getBillingForMember` (and the equivalent enrichment in
  `billing.web.BillingController` for the filtered path).
- **No caching** — every page recomputes from the database on each request.
- **Synchronous blocking integration calls** — `integration.*` services all `Thread.sleep` to
  simulate downstream latency and are called serially on the request thread (see
  `AdjudicationService`).
- **Duplicated business logic** — the approved-amount calculation exists in both
  `claims.service.AdjudicationService.computeApprovedCents` and
  `batch.ReconciliationService.expectedApprovedCents`; outstanding-balance math is duplicated
  between `billing.model.Invoice` and `billing.service.BillingService.totalOutstandingCents`.
- **Dead code after error paths** — e.g. the commented-out block after the `throw` in
  `AdjudicationService.adjudicate` is unreachable by design.

## Intentional testing behavior

- The suite is deliberately **sparse**. Most subsystems have no tests.
- `batch.ReconciliationTimingTest` is **flaky on purpose** (RNG seeded from the wall clock). It
  gives the demo a realistic occasionally-red test. Do not stabilize it unless that is the task.

## The flagship demo task

The headline scenario combines a **security fix and a cost/performance fix** that span multiple
files:

1. Enforce ownership/authorization on `GET /claims/{id}` (fix the IDOR, item 1 above), and
2. Eliminate the N+1 queries on the claims-list and billing paths.

A good fix touches the auth/authorization layer, `ClaimService`/`ClaimDetailController`, the
billing/claims repositories, and adds regression tests that prove both the access-control fix and
the reduced query count. See `demo/plan-mode-prompt.md` and `demo/trigger-artifact.md`.

## Ignore

- `db/schema.sql` and `db/seed.sql` are demo fixtures.
- `src/main/resources/templates/**` are Thymeleaf views; standard web review applies but do not
  expect a component framework.
- `demo/**` contains scripted demo prompts and a sample alert payload, not application code.
