# Review Guidelines

This is a **demonstration codebase**. The findings below document security remediation and
intentional performance/testing patterns so the repo can be used to explore security review,
performance analysis, and legacy modernization. Leave the performance and testing patterns in
place unless a task explicitly targets them.

## Remediated security findings

All seven security findings below have been remediated, each with a regression test under
`src/test/java`. Do not reintroduce them; the "Issue" column describes the original weakness for
context.

| # | Issue | CWE | Where | Status |
|---|-------|-----|-------|--------|
| 1 | **Broken access control / IDOR (flagship)** — the claim-detail endpoint loads a claim by id and renders it without verifying ownership, so any authenticated user can read any other member's claim by changing the id. | CWE-639 | `claims.web.ClaimDetailController.getClaim` (`GET /claims/{id}`) | Ownership check added while retaining audit logging. |
| 2 | **SQL injection** — the claims and billing status filters build SQL by string concatenation. | CWE-89 | `claims.repository.ClaimRepository.searchByStatus`, `billing.repository.BillingRepository.searchInvoices` | Rewritten with `PreparedStatement` parameters. |
| 3 | **Missing authentication for critical function** — the entire `/admin/**` area is not behind the auth interceptor; `/admin/users` dumps users + password hashes and `/admin/reconciliation/run` triggers a financial batch. | CWE-306 | `admin.web.AdminController`, `common.web.WebConfig` | `/admin/**` is behind `AuthInterceptor` and the handlers require `ADMIN`. |
| 4 | **Hardcoded credentials/secrets** — integration API key, fraud shared secret, and admin bootstrap password are hardcoded fallbacks. | CWE-798 | `src/main/resources/application.properties`, `common.config.AppConfig` | Values are required environment variables. |
| 5 | **Weak password hashing** — passwords are stored as unsalted MD5. | CWE-327, CWE-916 | `auth.service.PasswordHasher` | BCrypt is used and the seed hashes were refreshed. |
| 6 | **Path traversal** — the document download endpoint joins a caller-supplied filename to the storage root with no containment check. | CWE-22 | `document.service.DocumentService.readDocument`, `document.web.DocumentController` | Canonical-path containment is enforced. |
| 7 | **Known-vulnerable dependencies** — `log4j-core` / `log4j-api` 2.14.1 (Log4Shell, CVE-2021-44228) and `commons-collections` 3.2.1 (CVE-2015-7501) are pinned so the CI dependency audit has something concrete to flag. | — | `pom.xml`, `integration.NotificationService`, `batch.ReconciliationService` | Updated to log4j 2.23.1 and commons-collections4 4.4. |

The audit is expected to pass; do not add suppressions.

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
