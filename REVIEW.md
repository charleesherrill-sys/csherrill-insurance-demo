# Review Guidelines

This is a **demonstration codebase**. The weaknesses below are seeded on purpose so the repo can
be used to explore security review, performance analysis, and legacy modernization. Every item
here is intentional and should **not** be flagged as an accidental bug or "fixed" during a general
cleanup. When a task explicitly targets one of these, fix that specific issue and add a regression
test; leave the rest in place.

## Intentional security weaknesses

> **Remediation status (items 1–6):** the six application-level security weaknesses
> below have been **remediated**. They are retained in this table for historical
> context and to document what each fix addresses. Item 7 (pinned vulnerable
> dependencies) is intentionally left in place so the CI dependency audit still has
> something concrete to flag.

| # | Issue | CWE | Where | Status |
|---|-------|-----|-------|--------|
| 1 | **Broken access control / IDOR (flagship)** — the claim-detail endpoint loaded a claim by id and rendered it without verifying ownership, so any authenticated user could read any other member's claim by changing the id. | CWE-639 | `claims.web.ClaimDetailController.getClaim` (`GET /claims/{id}`) | ✅ Fixed — after loading the claim, `getClaim` compares `claim.getMemberUserId()` to the authenticated `user.getUserId()`. Members may only view their own claims; ADMIN/ADJUSTER (`UserSession.canViewAllMembers()`) may view any. Cross-account reads are audited and then rejected as not-found (no existence leak). |
| 2 | **SQL injection** — the claims and billing status filters built SQL by string concatenation. | CWE-89 | `claims.repository.ClaimRepository.searchByStatus`, `billing.repository.BillingRepository.searchInvoices` | ✅ Fixed — both queries now use `PreparedStatement` with `member_user_id` and `status` bound via `setLong`/`setString`. |
| 3 | **Missing authentication for critical function** — the entire `/admin/**` area was not behind the auth interceptor; `/admin/users` returns users and `/admin/reconciliation/run` triggers a financial batch. | CWE-306 | `admin.web.AdminController`, `common.web.WebConfig` | ✅ Fixed — `/admin/**` is now covered by `AuthInterceptor` (authentication required), and every admin handler enforces the ADMIN role via `requireAdmin(...)` (403 otherwise). |
| 4 | **Hardcoded credentials/secrets** — integration API key, fraud shared secret, and admin bootstrap password were hardcoded fallbacks. | CWE-798 | `src/main/resources/application.properties`, `common.config.AppConfig` | ✅ Fixed — these are injected from environment variables (`AEGIS_PAYMENT_GATEWAY_API_KEY`, `AEGIS_FRAUD_SHARED_SECRET`, `AEGIS_ADMIN_BOOTSTRAP_PASSWORD`) with no insecure defaults; startup fails fast if a required secret is absent. |
| 5 | **Weak password hashing** — passwords were stored as unsalted MD5. | CWE-327, CWE-916 | `auth.service.PasswordHasher` | ✅ Fixed — new passwords use salted, adaptive PBKDF2-HMAC-SHA256 in a self-describing format. `matches(...)` still verifies legacy MD5 hashes so pre-existing accounts (see migration note below) can log in; `isLegacyHash(...)` flags hashes needing upgrade. |
| 6 | **Path traversal** — the document download endpoint joined a caller-supplied filename to the storage root with no containment check. | CWE-22 | `document.service.DocumentService.readDocument`, `document.web.DocumentController` | ✅ Fixed — `DocumentService.resolveWithinRoot(...)` normalizes the path and rejects absolute paths and any name that escapes the storage root; the controller maps traversal attempts to 400 and missing files to 404. |
| 7 | **Known-vulnerable dependencies** — `log4j-core` / `log4j-api` 2.14.1 (Log4Shell, CVE-2021-44228) and `commons-collections` 3.2.1 (CVE-2015-7501) are pinned so the CI dependency audit has something concrete to flag. | — | `pom.xml`, `integration.NotificationService`, `batch.ReconciliationService` | ⛔ Intentionally retained (out of scope for this remediation). |

The empty `owasp-suppressions.xml` is intentional: the audit is meant to fail loudly. Do not add
suppressions to make it pass.

### Migration note for item 5 (existing MD5 hashes)

Stored MD5 hashes (including the seeded users in `db/seed.sql`) cannot be converted to PBKDF2
without the cleartext password. To preserve access, `PasswordHasher.matches(...)` continues to
verify legacy 32-hex MD5 hashes. Because the cleartext is available during a successful login,
the recommended migration is to transparently re-hash and persist with `PasswordHasher.hash(...)`
on next login (`isLegacyHash(...)` identifies which stored hashes still need upgrading). Seed
fixtures can also be regenerated with PBKDF2 values to remove MD5 entirely.

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
