# Review Guidelines

This is a **demonstration codebase**. The weaknesses below were seeded on purpose so the repo can
be used to explore security review, performance analysis, and legacy modernization. Items still
marked *intentional* should **not** be flagged as an accidental bug or "fixed" during a general
cleanup. When a task explicitly targets one of these, fix that specific issue and add a regression
test; leave the rest in place.

## Security weaknesses

Items 1–5 were explicitly requested to be fixed and are now **remediated**. Items 6–7 remain
intentional demo artifacts.

| # | Issue | CWE | Where | Status |
|---|-------|-----|-------|--------|
| 1 | **Broken access control / IDOR (flagship)** — the claim-detail endpoint loaded a claim by id and rendered it without verifying ownership. | CWE-639 | `claims.web.ClaimDetailController.getClaim` (`GET /claims/{id}`) | ✅ Fixed — ownership/role check; returns 403 for unauthorized cross-account access. |
| 2 | **SQL injection** — the claims and billing status filters built SQL by string concatenation. | CWE-89 | `claims.repository.ClaimRepository.searchByStatus`, `billing.repository.BillingRepository.searchInvoices` | ✅ Fixed — parameterized `PreparedStatement` with bound values. |
| 3 | **Missing authentication for critical function** — the entire `/admin/**` area was not behind the auth interceptor; `/admin/users` dumps users + password hashes and `/admin/reconciliation/run` triggers a financial batch. | CWE-306 | `admin.web.AdminController`, `common.web.WebConfig` | ✅ Fixed — `/admin/**` is intercepted and every handler requires the `ADMIN` role (403 otherwise). |
| 4 | **Hardcoded credentials/secrets** — integration API key, fraud shared secret, and admin bootstrap password were hardcoded fallbacks. | CWE-798 | `src/main/resources/application.properties`, `common.config.AppConfig` | ✅ Fixed — required from env vars with no default; startup fails if absent/blank. |
| 5 | **Weak password hashing** — passwords were stored as unsalted MD5. | CWE-327, CWE-916 | `auth.service.PasswordHasher` | ✅ Fixed — salted, adaptive BCrypt; legacy MD5 verified and upgraded on login (see Migration below). |
| 6 | **Path traversal** — the document download endpoint joins a caller-supplied filename to the storage root with no containment check. | CWE-22 | `document.service.DocumentService.readDocument`, `document.web.DocumentController` | Intentional (unchanged) |
| 7 | **Known-vulnerable dependencies** — `log4j-core` / `log4j-api` 2.14.1 (Log4Shell, CVE-2021-44228) and `commons-collections` 3.2.1 (CVE-2015-7501) are pinned so the CI dependency audit has something concrete to flag. | — | `pom.xml`, `integration.NotificationService`, `batch.ReconciliationService` | Intentional (unchanged) |

The empty `owasp-suppressions.xml` is intentional: the audit is meant to fail loudly. Do not add
suppressions to make it pass.

### Password-hash migration (item 5)

Existing rows in the `users` table hold 32-char unsalted MD5 digests. The migration is
zero-downtime and requires no batch job:

- `PasswordHasher.hash` now produces BCrypt hashes; new/changed passwords are stored as BCrypt.
- `PasswordHasher.matches` detects a legacy MD5 hash (32 hex chars) and verifies it with the old
  digest, so pre-existing accounts can still log in.
- On a successful login against a legacy hash, `AuthService.authenticate` re-hashes the verified
  cleartext with BCrypt and persists it via `UserRepository.updatePasswordHash`, so each account is
  upgraded the next time its owner signs in.
- The `users.password_hash` column was widened to `VARCHAR(72)` to hold 60-char BCrypt hashes.
- Seed data in `db/seed.sql` still ships MD5 digests; those accounts upgrade to BCrypt on first
  login via the same path.

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

1. Enforce ownership/authorization on `GET /claims/{id}` (fix the IDOR, item 1 above) — **done**
   (see item 1 status), and
2. Eliminate the N+1 queries on the claims-list and billing paths — still open.

A good fix touches the auth/authorization layer, `ClaimService`/`ClaimDetailController`, the
billing/claims repositories, and adds regression tests that prove both the access-control fix and
the reduced query count. See `demo/plan-mode-prompt.md` and `demo/trigger-artifact.md`.

## Ignore

- `db/schema.sql` and `db/seed.sql` are demo fixtures.
- `src/main/resources/templates/**` are Thymeleaf views; standard web review applies but do not
  expect a component framework.
- `demo/**` contains scripted demo prompts and a sample alert payload, not application code.
