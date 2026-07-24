# Review Guidelines

This is a **demonstration codebase**. The weaknesses below are seeded on purpose so the repo can
be used to explore security review, performance analysis, and legacy modernization. Every item
here is intentional and should **not** be flagged as an accidental bug or "fixed" during a general
cleanup. When a task explicitly targets one of these, fix that specific issue and add a regression
test; leave the rest in place.

## Intentional security weaknesses

The following are still seeded on purpose and remain in place:

| # | Issue | CWE | Where |
|---|-------|-----|-------|
| 4 | **Hardcoded credentials/secrets** — integration API key, fraud shared secret, and admin bootstrap password are hardcoded fallbacks. | CWE-798 | `src/main/resources/application.properties`, `common.config.AppConfig` |
| 6 | **Path traversal** — the document download endpoint joins a caller-supplied filename to the storage root with no containment check. | CWE-22 | `document.service.DocumentService.readDocument`, `document.web.DocumentController` |
| 7 | **Known-vulnerable dependencies** — `log4j-core` / `log4j-api` 2.14.1 (Log4Shell, CVE-2021-44228) and `commons-collections` 3.2.1 (CVE-2015-7501) are pinned so the CI dependency audit has something concrete to flag. | — | `pom.xml`, `integration.NotificationService`, `batch.ReconciliationService` |

The empty `owasp-suppressions.xml` is intentional: the audit is meant to fail loudly. Do not add
suppressions to make it pass.

## Resolved security findings (auth-flow hardening)

These previously-seeded weaknesses have been **fixed** as an explicit hardening task. They should
no longer be treated as intentional artifacts.

| # | Issue | CWE | Fix |
|---|-------|-----|-----|
| 1 | **Broken access control / IDOR (flagship)** — the claim-detail endpoint returned a claim regardless of ownership. | CWE-639 | `claims.web.ClaimDetailController.getClaim` now allows access only when the caller owns the claim or `UserSession.canViewAllMembers()` (ADJUSTER/ADMIN) is true; otherwise it returns HTTP 403 + `claims/not-authorized` and audits a `CLAIM_VIEW_DENIED`. The same ownership/role check was added to the `POST /claims/{id}/adjudicate` action in `ClaimIntakeController`. |
| 2 | **SQL injection** — the claims and billing status filters built SQL by string concatenation. | CWE-89 | `ClaimRepository.searchByStatus` and `BillingRepository.searchInvoices` now bind the member id and status via `PreparedStatement` parameters. |
| 3 | **Missing authentication for critical function** — the entire `/admin/**` area was unauthenticated. | CWE-306 | `common.web.WebConfig` now registers `/admin/**` with the `AuthInterceptor`, which enforces ADMIN-only access for admin paths. `AdminService.listAllUsers` no longer returns password hashes. |
| 5 | **Weak password hashing** — passwords were stored as unsalted MD5. | CWE-327, CWE-916 | `auth.service.PasswordHasher` now uses salted BCrypt (`spring-security-crypto`). Legacy MD5 hashes still verify and are transparently upgraded to BCrypt on the next successful login (`AuthService`); `db/seed.sql` ships BCrypt hashes. |
| — | **Session management** — in-memory sessions never expired and the cookie was not `Secure`. | CWE-613, CWE-614, CWE-384 | `SessionManager` enforces idle (30 min) and absolute (8 h) timeouts and mints a fresh session id per login (fixation protection); `LoginController` sets the `AEGIS_SESSION` cookie `Secure` (configurable via `aegis.session.cookie.secure`, default true) in addition to `HttpOnly`. |

Regression tests live under `src/test/java/com/aegis/auth` (`PasswordHasherTest`, `AuthServiceTest`,
`AuthInterceptorTest`, `SessionManagerTest`) and `src/test/java/com/aegis/claims/web`
(`ClaimDetailControllerTest`).

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

1. Enforce ownership/authorization on `GET /claims/{id}` (fix the IDOR) — **now resolved**, see
   "Resolved security findings" above, and
2. Eliminate the N+1 queries on the claims-list and billing paths (still open).

A good fix touches the auth/authorization layer, `ClaimService`/`ClaimDetailController`, the
billing/claims repositories, and adds regression tests that prove both the access-control fix and
the reduced query count. See `demo/plan-mode-prompt.md` and `demo/trigger-artifact.md`.

## Ignore

- `db/schema.sql` and `db/seed.sql` are demo fixtures.
- `src/main/resources/templates/**` are Thymeleaf views; standard web review applies but do not
  expect a component framework.
- `demo/**` contains scripted demo prompts and a sample alert payload, not application code.
