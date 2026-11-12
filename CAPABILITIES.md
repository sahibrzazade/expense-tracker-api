# Capability Map: Expense Tracker API

Backend REST API for a personal expense tracker. A separate frontend repository will consume this API later.

| Module id | Responsibility | Depends on |
|---|---|---|
| `foundation` | Project skeleton, PostgreSQL + Flyway, error format, CORS, OpenAPI, health, test infrastructure | — |
| `identity` | Register, login, JWT issuance/validation, current user (`/me`), user's currency | foundation |
| `categories` | Default and user-defined categories, CRUD | identity |
| `expenses` | Expense CRUD with pagination and filtering (date range, category, amount) | identity, categories |
| `reports` | Monthly total, category breakdown, monthly trend | expenses |
| `budgets` | Monthly overall and per-category budgets, spent/remaining, over-budget status | expenses |

**Build order:** `foundation` → `identity` → `categories` → `expenses` → `reports`, `budgets` (the last two are independent of each other)

Each module has its own spec in this directory: `SPEC-<module-id>.md`.

## Cross-cutting decisions

These apply to every module. Module specs must not contradict them without updating this file first.

| Topic | Decision |
|---|---|
| Repositories | Two separate repos: `backend` (this one) and `frontend`. The OpenAPI document is the contract between them. |
| API style | REST + JSON, all endpoints under `/api/v1` |
| Errors | RFC 9457 `ProblemDetail` (`application/problem+json`) |
| Auth | Stateless JWT access token (no refresh token in MVP), BCrypt password hashing |
| Data isolation | A user only sees their own data; accessing another user's resource returns `404` |
| Currency | One currency per user (`AZN` or `USD`), chosen at registration. No exchange-rate conversion. |
| Money | `BigDecimal` in Java, `NUMERIC(19,2)` in PostgreSQL, serialized as a JSON number with 2 decimal places; amounts must be `> 0` |
| Identifiers | UUID primary keys |
| Time | Expense date is `LocalDate`; audit timestamps are `Instant` (UTC) |
| Category deletion | Deleting a category that still has expenses returns `409 Conflict` |
| Budgets | Defined per month (e.g. `2026-11`), not as a recurring limit |

## Out of scope for MVP

Frontend, deployment and CI/CD, email verification, password reset, refresh tokens, multi-currency per user, exchange rates.
