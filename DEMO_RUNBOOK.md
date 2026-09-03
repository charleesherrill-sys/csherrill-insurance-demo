# Devin Demo Runbook — Aegis Claims Platform (+ License Manager warm-up)

Everything in this runbook was verified end-to-end on 2026-09-03 (Docker Compose v2.32, Maven
3.6, Java 8 in-container / Java 11 on host, Python 3). Times quoted are from that run.

The repo's seeded weaknesses are **intentional demo material** (see `REVIEW.md`, `AGENTS.md`).
Nothing in this runbook fixes them — Devin fixes them live, on a branch, during the demo.

---

## 0. Access & prep (do this the day before)

| Item | Status / action |
|------|-----------------|
| `charleesherrill-sys/csherrill-insurance-demo` | Devin's GitHub app has **write** access (push verified). Every demo run creates a `devin/<ts>-...` branch + PR here. There are already ~10 stale `devin/*` branches from earlier runs — delete them before the demo so the prospect sees a clean repo, or work from a **fork** if you don't want to touch Charlee's originals. |
| `charleesherrill-sys/csherrill-demo-repo` | Public clone works, but Devin **cannot push** (HTTP 403). Fork it into an org Devin has access to (or ask Charlee to add the Devin app) before demoing the Python fix; otherwise Devin can only show the diff, not open a PR. |
| DeepWiki | `.devin/wiki.json` is valid; index the repo in DeepWiki ahead of time (indexing is not instant). |
| Docker Hub | The original `Dockerfile` used `openjdk:8-jre-slim`, which has been removed from Docker Hub. This branch swaps it for `eclipse-temurin:8-jre`; without that swap `docker-compose up --build` fails immediately with `manifest unknown`. |
| OWASP audit | Needs outbound access to `nvd.nist.gov`. In locked-down sandboxes it fails with `403 Forbidden` on the NVD feed **before** scanning (see §2.3). Test on your laptop network first. |

## 1. Environment setup (≈ 3 min)

```bash
git clone https://github.com/charleesherrill-sys/csherrill-insurance-demo.git
cd csherrill-insurance-demo
docker compose up --build -d          # first build ≈ 1–2 min (Maven downloads); later runs seconds
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/login   # → 200
```

Postgres (`aegis-db`, port 5432, user/pass `aegis`/`aegis_dev_password`) is seeded automatically
from `db/schema.sql` + `db/seed.sql`. App is `aegis-app` on http://localhost:8080.

Reset between demos: `docker compose down -v && docker compose up --build -d`
(the `-v` drops the audit_log rows you create during the demo).

### Seeded logins (all verified)

| Username | Password | Role | User id | Notes |
|----------|----------|------|---------|-------|
| `amorgan` | `password` | MEMBER | 5583 | Owns claim **90233** (CLM-90233, $1,845 billed / $1,520 approved). 3 claims, 2 invoices. |
| `bhopkins` | `password` | MEMBER | 4471 | Owns claim **90311**. The "attacker" in the IDOR story. |
| `cwright` | `password` | MEMBER | 6001 | |
| `dpatel` | `claims2015` | MEMBER | 6002 | |
| `jadjuster` | `letmein` | ADJUSTER | 2 | Legitimately allowed to see any claim — useful when discussing the fix. |
| `admin` | `admin123` | ADMIN | 1 | |

Browser flow: `/login → /dashboard → /claims → /claims/{id} → /billing`.

## 2. Pre-flight checks (run once, off-camera)

### 2.1 Reproduce the flagship IDOR (30 s)

1. Log in as `bhopkins` / `password`.
2. Browse to http://localhost:8080/claims/90233 — Alex Morgan's medical claim renders in full
   (diagnosis `J20.9`, line items, approved amount). **200 OK, no error.**
3. Prove the app *knows* it's wrong but does nothing:
   ```bash
   docker exec aegis-db psql -U aegis -d aegis -c "select * from audit_log order by id desc limit 3;"
   #  4471 | CLAIM_VIEW | claim | 90233 | cross-account read: viewer 4471 owner 5583
   ```
   That audit row is exactly what `demo/trigger-artifact.md` is modelled on.

Root cause: `com.aegis.claims.web.ClaimDetailController.getClaim` loads by id and never compares
`claim.getMemberUserId()` to the session user (CWE-639). `AuthInterceptor` only proves you're
logged in.

### 2.2 Reproduce the N+1 (1 min)

```bash
docker exec aegis-db psql -U aegis -d aegis -c "ALTER SYSTEM SET log_statement='all';"
docker exec aegis-db psql -U aegis -d aegis -c "SELECT pg_reload_conf();"
# then, logged in as amorgan, load /claims and /billing and count:
docker logs aegis-db 2>&1 | grep -c 'execute .*SELECT'
```

Measured on the seed data (amorgan: 3 claims, 2 invoices):

| Page | SELECTs | Shape |
|------|---------|-------|
| `GET /claims` | **7** | 1 list + (lines + policy) × 3 claims |
| `GET /billing` | **10** | 2 × [1 list + (payments + policy) × 2 invoices] — `BillingController` calls `getBillingForMember` **twice** (page + `totalOutstandingCents`) |

Talking point: linear in data volume. A member with 200 claims = 401 round trips per page load.
Code: `ClaimService.getClaimsForMember`, `BillingService.getBillingForMember`.

### 2.3 Build, tests, audit

```bash
mvn clean package                                   # BUILD SUCCESS ≈ 15 s, 9 tests
mvn -Paudit org.owasp:dependency-check-maven:check  # expected BUILD FAILURE
```

- Tests: `PasswordHasherTest`(3), `EligibilityServiceTest`(3), `InvoiceMathTest`(2),
  `ReconciliationTimingTest`(1). The last one is **flaky by design** (wall-clock-seeded RNG):
  over 3 runs it failed twice ("reconciliation pass rate too low: 0.85"). Good aside if the
  prospect asks "what does Devin do with flaky tests?" — it should recognise it from
  `AGENTS.md`/`REVIEW.md` and *not* silently stabilise it.
- Audit: with network access it fails on `log4j-core`/`log4j-api 2.14.1` (CVE-2021-44228,
  Log4Shell) and `commons-collections 3.2.1` (CVE-2015-7501), CVSS ≥ 7 threshold. Without NVD
  access it fails earlier with `403 Forbidden ... NoDataException` — same red outcome, different
  reason; know which one you're showing.
- CI (`.github/workflows/ci.yml`) mirrors this: build job + a separate audit job that is *expected*
  red. Don't let the prospect read a red audit check as "Devin broke it".

### 2.4 Python warm-up repo

```bash
git clone https://github.com/charleesherrill-sys/csherrill-demo-repo.git && cd csherrill-demo-repo
pip install pytest && python -m pytest -v
# 1 failed, 3 passed — tests/test_licenses.py::test_provision_exact_capacity  ValueError: Not enough seats
```

Bug: `Product.provision` in `license_manager.py` uses `>=` where it should use `>`, so a customer
can never consume their last seat.

---

## 3. Demo script (≈ 25–30 min)

Suggested arc: **understand → plan → fix (event-driven) → scale**. Each stage below has the exact
prompt, what Devin should produce, and what to say while it works.

### Stage 0 — (Optional, 3 min) Warm-up: Python off-by-one

Use this if the audience is new to Devin or you want a fast, guaranteed win before the monolith.

Prompt (Devin, on the demo-repo fork):
> `python -m pytest` has one failing test. Find the root cause, fix it, and open a PR.

Expected: Devin runs pytest, reads the traceback, changes `>=` → `>` in `Product.provision`,
re-runs (4 passed), opens a 1-line PR. ~2–3 min.

Talking points: Devin reproduces before fixing; the fix is minimal; the PR is reviewable. Segue:
"That's a toy. Let's do this on a 10-year-old Java monolith with no ORM."

### Stage 1 — Understand the codebase (DeepWiki + Ask Devin, 5 min)

1. Open the DeepWiki for the repo. Show the **Claims Adjudication** and **Security Notes** pages —
   both call out `ClaimDetailController.getClaim` by name (that's steered by `.devin/wiki.json`).
2. Ask Devin (`demo/ask-devin-question.md`):
   > How does claim adjudication work in this system end-to-end — from intake to payment — and
   > where would a security or data-exposure risk most likely show up in that path?

Expected answer: traces `ClaimIntakeService.submit → validate` (eligibility) →
`AdjudicationService.adjudicate` (fraud score, `computeApprovedCents`) → `triggerPayment` →
`PaymentService` → `PaymentGatewayClient`; flags that `AuthInterceptor` enforces authentication
only and that `GET /claims/{id}` has no ownership check; likely also mentions SQLi in
`searchByStatus` and the unauthenticated `/admin/**`.

3. Second Ask Devin (scoping — sets up Stage 3):
   > Customers report the claim-detail page sometimes shows another member's claim, and the claims
   > and billing pages are slow. Scope the fix: which files change, and what regression tests
   > would prove it?

Expected: names `ClaimDetailController`, `ClaimService`, `BillingService`, the two repositories,
and proposes an ownership test + a query-count test — i.e. the plan Devin will execute next.

Talking points:
- Nine subsystems, raw JDBC, Thymeleaf — "this is what your estate actually looks like".
- Devin answers in terms of *your* class names and flow, not generic advice.
- DeepWiki is regenerated from code, so docs don't rot.

### Stage 2 — Show the bug live (2 min)

In the browser: log in as `bhopkins`, open `/claims/90233`, show Alex Morgan's claim. Then show
the `audit_log` row (§2.1). Optionally show the Postgres statement log scrolling for `/billing`
(§2.2).

Talking point: "The system logs the breach and serves the page anyway. And every list page is
N+1 — this is the security *and* the cloud-bill story in one PR."

### Stage 3 — Event-driven fix: alert → Devin → PR (12–15 min, the flagship)

Pick **one** trigger:

**(a) Production-alert trigger** — paste `demo/trigger-artifact.md` into Slack/`@Devin` or the API
(`POST /v1/sessions`). It's a Sentry-style payload:
`AuthorizationError: user 4471 accessed claim 90233 belonging to user 5583` in
`ClaimDetailController.getClaim`, plus a p95 latency breach on `/claims` and `/billing`.

**(b) Plan-mode trigger** — paste `demo/plan-mode-prompt.md` as a new session with planning
enabled. Devin produces a plan first (good for audiences who want to see the human approval gate),
you approve, then it executes.

Expected Devin behaviour (both triggers):
1. Reads `AGENTS.md`/`REVIEW.md`, brings up Postgres + app (or unit-tests without DB).
2. **Reproduces** the IDOR (curl as 4471 → 200 on 90233) and the N+1 (query count).
3. Fix 1 — authorization: in `ClaimDetailController.getClaim` (or a helper in `ClaimService`),
   if the session role is MEMBER and `claim.getMemberUserId() != user.getUserId()`, return
   403/not-found; ADJUSTER/ADMIN still allowed. Keeps the audit row.
4. Fix 2 — N+1: new batched repository methods (`findLinesByClaimIds(List<Long>)`,
   `findPaymentsByInvoiceIds(...)`, `PolicyRepository.findByIds(...)`, or JOINs) so
   `getClaimsForMember`/`getBillingForMember` run a constant ~3 queries; ideally also stops
   `BillingController` computing the billing list twice.
5. **Regression tests**: an access-control test (member reading another member's claim is
   rejected, adjudicator allowed) and a query-count test (counting repository calls with a
   stub/mock or a counting `Database` wrapper).
6. `mvn clean package` green (may need one retry if `ReconciliationTimingTest` flakes — watch
   Devin reason about that), branch `devin/<ts>-...`, PR opened, link posted back to the thread.

Typical wall-clock: 8–15 min. Have Stage 1/2 material or Q&A ready while it runs.

Talking points while Devin works:
- Reproduce → fix → verify → regression test: same discipline you'd expect from a senior engineer.
- It respected `REVIEW.md` and touched only the two targeted issues (check the PR diff for this —
  SQLi, MD5, `/admin` are all still there, on purpose).
- Multi-file, cross-layer change (web + service + repository + tests) in one coherent PR.
- CI: the build job goes green; the dependency-audit job stays red *by design* — say that before
  the prospect notices.
- Cost angle: N+1 elimination = fewer DB round trips = lower RDS/latency spend; ownership check =
  avoided breach-notification cost.

Verify live once the PR is up (optional but powerful):
```bash
git fetch origin && git checkout <devin-branch> && docker compose up --build -d
# bhopkins → /claims/90233 now 403/404; amorgan → 200; SELECT count on /claims drops from 7 to ~3
```

### Stage 4 — Scale-out follow-ups (5 min, pick per audience)

- **Security at scale**: "Now do the rest of `REVIEW.md`" — fan out child sessions, one per CWE
  (SQLi in `searchByStatus`/`searchInvoices`, unauthenticated `/admin/**`, path traversal in
  `DocumentService.readDocument`, MD5 → bcrypt, hardcoded secrets, log4j/commons-collections
  upgrade). Each produces its own PR; the audit job finally goes green after the dependency PR.
- **Modernisation**: "Plan a Java 8 / Spring Boot 2.3 → Java 21 / Boot 3 upgrade" — inventory,
  ordered plan, one PR per slice.
- **Tech-debt**: de-duplicate `computeApprovedCents` vs `expectedApprovedCents`; make
  `ReconciliationTimingTest` deterministic (explicitly as *the* task).

---

## 4. Known gotchas

| Symptom | Cause / fix |
|---------|-------------|
| `docker compose up --build` → `openjdk:8-jre-slim: not found` | Old base image removed from Docker Hub. Use this branch's `Dockerfile` (`eclipse-temurin:8-jre`). |
| `ALTER SYSTEM cannot run inside a transaction block` | Run `ALTER SYSTEM` and `pg_reload_conf()` as two separate `psql -c` calls. |
| `mvn clean package` red on `ReconciliationTimingTest` | Intentional flake (~50 % in our runs). Re-run; do not let Devin "fix" it unless asked. |
| Audit fails with `403 Forbidden` from nvd.nist.gov | Network-restricted environment; run on a network that can reach NVD (or set `NVD_API_KEY`). Failure is expected either way, but the CVE report only exists in the first case. |
| Devin can't push to `csherrill-demo-repo` | Devin's GitHub app isn't installed there. Fork it or have Charlee grant access. |
| Stale `devin/*` branches | Previous runs; prune before the demo. |
| Host Java is 11, repo targets 8 | `mvn clean package` compiles fine on 11 (`release 8`); the Docker build uses real Java 8. |

## 5. Quick reference

```
App        http://localhost:8080          DB   localhost:5432  aegis / aegis_dev_password
IDOR       bhopkins/password → /claims/90233 (owner amorgan 5583)   → 200 (should be 403)
N+1        amorgan → /claims = 7 SELECTs, /billing = 10 SELECTs
Prompts    demo/ask-devin-question.md · demo/plan-mode-prompt.md · demo/trigger-artifact.md
Fix scope  ClaimDetailController.getClaim · ClaimService.getClaimsForMember · BillingService.getBillingForMember
Do NOT fix SQLi, /admin auth, MD5, path traversal, hardcoded secrets, log4j — unless that's the task
```
