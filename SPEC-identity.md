# Spec: identity

Module id: `identity` · Depends on: `foundation` · See [CAPABILITIES.md](CAPABILITIES.md)

## Objective

Let a person create an account, sign in, stay signed in, and sign out, and let every other module know **who** is calling. After this module, all `/api/**` endpoints except auth require a valid access token, and later modules can read the current user's id and currency.

## Decisions

| Topic | Decision |
|---|---|
| User fields | `email`, `password`, `name`, `currency` (`AZN` or `USD`) |
| Currency | Chosen at registration, **cannot be changed** |
| Access token | JWT, HS256, **15 minutes**, sent as `Authorization: Bearer <token>` |
| Refresh token | Opaque random value (not a JWT), **7 days**, stored only as a SHA-256 hash, **rotated on every use** |
| Refresh token transport | `httpOnly` cookie; never in a JSON body |
| Register response | Signs the user in (returns tokens), same as login |
| Email | Trimmed, stored lower-case, unique |
| Password | 8–72 characters (72 is the BCrypt input limit), hashed with BCrypt |

## API

All bodies are JSON; all errors are ProblemDetail (see `SPEC-foundation.md`).

| Method & path | Auth | Request | Success |
|---|---|---|---|
| `POST /api/v1/auth/register` | none | `{email, password, name, currency}` | `201` + `AuthResponse` + refresh cookie |
| `POST /api/v1/auth/login` | none | `{email, password}` | `200` + `AuthResponse` + refresh cookie |
| `POST /api/v1/auth/refresh` | refresh cookie | — | `200` + `AuthResponse` + **new** refresh cookie |
| `POST /api/v1/auth/logout` | refresh cookie (optional) | — | `204`, refresh token revoked, cookie cleared |
| `GET /api/v1/me` | access token | — | `200` + `UserResponse` |

```json
// AuthResponse
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": { "id": "3f2c...", "email": "sahib@example.com", "name": "Sahib", "currency": "AZN" }
}
```

### Errors

| Situation | Status |
|---|---|
| Invalid body (bad email, short password, missing name, unknown currency) | `400` with `errors` |
| Email already registered | `409` |
| Wrong email or password | `401`, detail `"Invalid email or password"` (same for both cases) |
| Missing, invalid, expired or revoked refresh token | `401` |
| Missing, invalid or expired access token on a protected endpoint | `401` |

### Refresh cookie

| Attribute | Value |
|---|---|
| Name | `refresh_token` |
| `HttpOnly` | yes (JavaScript cannot read it) |
| `Secure` | yes (browsers allow it on `http://localhost`) |
| `SameSite` | `Strict` by default, configurable (`Lax`/`None` if frontend and API end up on different sites) |
| `Path` | `/api/v1/auth` (only sent to auth endpoints) |
| `Max-Age` | 7 days; `0` on logout |

Because the cookie is sent by the browser, the frontend calls auth endpoints with `credentials: "include"`, and CORS must allow credentials for the configured origins.

### Refresh token rotation and reuse detection

1. Each refresh token can be used **once**. A successful `/refresh` revokes it and issues a new one.
2. If an already-revoked token is presented again, it was likely stolen: **all** of that user's refresh tokens are revoked and the request gets `401`. The user must log in again.
3. Expired tokens are rejected. Cleaning up old rows is out of scope for MVP.

## Data model

New Flyway migrations `V2__create_users.sql` and `V3__create_refresh_tokens.sql`:

```sql
users (
  id            UUID PRIMARY KEY,
  email         VARCHAR(254) NOT NULL UNIQUE,   -- stored lower-case
  name          VARCHAR(100) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  currency      VARCHAR(3)   NOT NULL CHECK (currency IN ('AZN', 'USD')),
  created_at    TIMESTAMPTZ  NOT NULL
)

refresh_tokens (
  id          UUID PRIMARY KEY,
  user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash  CHAR(64)    NOT NULL UNIQUE,      -- SHA-256 hex of the raw token
  expires_at  TIMESTAMPTZ NOT NULL,
  revoked_at  TIMESTAMPTZ,                      -- null = still usable
  created_at  TIMESTAMPTZ NOT NULL
)
```

## Security setup

- **Dependencies (approved with this spec):** `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server` (JWT validation and Nimbus JWT encoding), `spring-security-test` (test scope).
- **Stateless:** no HTTP session; CSRF protection disabled because protected endpoints use a bearer header, and the refresh cookie is `SameSite=Strict` and scoped to `/api/v1/auth`.
- **Public endpoints:** `/api/v1/auth/**`, `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`. Everything else requires a valid access token.
- **CORS** is enabled through `http.cors(...)` using the existing `CorsConfig`, now with `allowCredentials(true)`.
- **401 / 403** from Spring Security are returned as ProblemDetail, like every other error.
- **JWT claims:** `sub` = user id, `iss` = `expense-tracker-api`, `iat`, `exp`. Signing secret from `JWT_SECRET` (at least 32 bytes); the app refuses to start without it. The `dev` and `test` profiles supply local-only secrets.
- **Login timing:** when the email does not exist, still run a BCrypt check against a dummy hash so response time does not reveal which emails are registered.
- **Current user for other modules:** a small `CurrentUser` helper exposes the authenticated user's id; services load the user (and currency) from it.

## Configuration

| Property | Env var | Default |
|---|---|---|
| `app.auth.jwt-secret` | `JWT_SECRET` | none (required) |
| `app.auth.access-token-ttl` | — | `15m` |
| `app.auth.refresh-token-ttl` | — | `7d` |
| `app.auth.refresh-cookie-same-site` | `REFRESH_COOKIE_SAME_SITE` | `Strict` |

## Project Structure

```
com.expensetracker.identity
├── User, UserRepository                 → JPA entity + Spring Data repository
├── RefreshToken, RefreshTokenRepository
├── AuthService                          → register, login, refresh, logout
├── TokenService                         → issue/verify access JWTs, create/hash refresh tokens
├── AuthController, MeController         → HTTP layer + request/response records
├── AuthProperties                       → app.auth.* settings
├── CurrentUser                          → helper for other modules
└── security/SecurityConfig              → filter chain, JWT encoder/decoder, 401/403 handlers
```

## Testing Strategy

Integration tests (`IntegrationTest`, real PostgreSQL) for every endpoint and error row above, plus:
- the refresh cookie attributes (`HttpOnly`, `Secure`, `SameSite`, `Path`, `Max-Age`);
- rotation (old token rejected after use) and reuse detection (all sessions revoked);
- an expired access token is rejected (clock controlled in the test);
- a protected endpoint without a token returns `401` ProblemDetail;
- passwords are never stored or returned in plain text.

Unit tests for pure logic (token hashing, email normalization) where it keeps tests fast and focused.

## Boundaries

- **Always:** hash passwords (BCrypt) and refresh tokens (SHA-256); return the same error for wrong email and wrong password; keep secrets in env vars.
- **Ask first:** changing token lifetimes, cookie attributes or the public endpoint list; adding OAuth/social login.
- **Never:** log passwords, tokens or token hashes; put the refresh token in a JSON body or `localStorage`; return `password_hash` in any response.

## Success Criteria

1. Register returns `201`, an access token, the user, and a refresh cookie with the attributes above; the password is stored only as a BCrypt hash.
2. Registering an existing email (any letter case) returns `409`.
3. Login with correct credentials returns `200` with tokens; wrong email and wrong password both return the same `401`.
4. `GET /api/v1/me` returns the user with a valid token and `401` without, with an invalid, or with an expired token.
5. `/refresh` returns a new access token and a new refresh cookie; the old refresh token then fails with `401`.
6. Reusing a revoked refresh token returns `401` and revokes all of that user's refresh tokens.
7. `/logout` returns `204`, clears the cookie, and the refresh token no longer works.
8. A browser preflight from the allowed origin to an auth endpoint succeeds with `Access-Control-Allow-Credentials: true`.
9. Swagger UI and `/v3/api-docs` stay reachable without a token; the docs show bearer auth on protected endpoints.
10. `./mvnw verify` passes.

## Out of Scope

Email verification, password reset, changing email/password/currency, deleting an account, rate limiting login attempts, cleanup of expired refresh tokens, social login.

## Open Questions

None.
