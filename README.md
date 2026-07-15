# Aegis Claims Platform

Aegis is a fictional legacy insurance and benefits platform covering **claims, policy, and
billing**. This repository is a small, self-contained, runnable **extract** of a much larger
legacy estate: think of it as a few subsystems carved out of a monolith that has grown to
millions of lines over more than a decade. It is deliberately built the way that estate would
actually look, so it is useful for exploring, documenting, and modernizing legacy code.

> This is a demonstration codebase. It intentionally contains security weaknesses,
> performance problems, and technical debt. See [`REVIEW.md`](REVIEW.md) and
> [`AGENTS.md`](AGENTS.md) before "fixing" anything.

## Why it looks the way it does

The stack is intentionally dated so the code feels like a real legacy system rather than a
greenfield sample:

- **Java 8** and **Spring Boot 2.3.x** (an older, out-of-support line)
- Server-rendered **Thymeleaf** templates with a little jQuery-era styling
- **Hand-written JDBC with raw SQL** in the hot paths (no ORM)
- **PostgreSQL** via `docker-compose`
- **Maven** build

There is no clean service mesh, no repository abstraction layer worth the name, duplicated
business logic between services, and a nightly batch job bolted onto the side. That is the point.

## Subsystems

The code is organized into nine packages under `com.aegis.*`, each a recognizable piece of a
claims platform:

| Package | Responsibility |
|---------|----------------|
| `auth` | Login, cookie sessions, role checks |
| `policy` | Policy lookups |
| `claims` | Claim intake, validation, adjudication (submit -> validate -> adjudicate -> pay) |
| `billing` | Invoices, payments, disbursement |
| `document` | Document upload/download |
| `admin` | Internal admin portal |
| `reporting` | Aggregate reporting |
| `batch` | Nightly reconciliation job |
| `integration` | Mocked third-party services (eligibility, fraud, payment gateway, notifications) |

The core business flow is claim adjudication, which spans `claims` and `billing`. Those two are
the hot paths and the most interesting part of the codebase.

## Running it

You need Docker and Docker Compose.

```bash
docker-compose up --build
```

This starts Postgres (schema and seed data are applied automatically) and the application.
Open http://localhost:8080 and sign in.

### Seeded users

| Username | Password | Role | User id |
|----------|----------|------|---------|
| `amorgan` | `password` | MEMBER | 5583 |
| `bhopkins` | `password` | MEMBER | 4471 |
| `cwright` | `password` | MEMBER | 6001 |
| `dpatel` | `claims2015` | MEMBER | 6002 |
| `jadjuster` | `letmein` | ADJUSTER | 2 |
| `admin` | `admin123` | ADMIN | 1 |

Member `amorgan` (5583) owns claim `90233`; member `bhopkins` (4471) owns claim `90311`. The two
member accounts make cross-account behavior easy to demonstrate.

### Browser flow

`login -> dashboard -> claims list -> claim detail -> billing`. Every page is reachable from the
top navigation once you are signed in.

## Building and testing without Docker

```bash
# Build and run the (sparse, partly flaky) test suite
mvn clean package

# Dependency / security audit (expected to find issues)
mvn -Paudit org.owasp:dependency-check-maven:check
```

The unit tests run without a database. To exercise the app itself you still need Postgres; the
easiest path is `docker-compose up`.

## Where to look first

- `com.aegis.claims.web.ClaimDetailController` serves `GET /claims/{id}`.
- `com.aegis.claims.service.ClaimService` and `com.aegis.billing.service.BillingService` build the
  claims-list and billing pages.
- `com.aegis.claims.service.AdjudicationService` runs the adjudicate-and-pay flow.
- `.devin/wiki.json` steers DeepWiki toward these hot paths.
- `demo/` holds the scripted demo prompts and a sample production alert.
