# Spec: foundation

Module id: `foundation` · Depends on: — · See [CAPABILITIES.md](CAPABILITIES.md)

## Objective

Provide a runnable, tested Spring Boot skeleton that every other module builds on. After this module, a developer can clone the repo, start PostgreSQL, run the app and the test suite, and see a consistent error format, health check, CORS policy and API documentation — before any business feature exists.

This module ships no business endpoints.

## Tech Stack

| Concern | Choice |
|---|---|
| Language | Java 25 (LTS) |
| Framework | Spring Boot 4.x — latest stable at scaffold time, verified on start.spring.io |
| Build | Maven via wrapper (`./mvnw`); no global Maven install needed |
| Web | Spring Web MVC |
| Validation | Jakarta Bean Validation (`spring-boot-starter-validation`) |
| Persistence | Spring Data JPA + PostgreSQL JDBC driver |
| Migrations | Flyway (`flyway-core` + `flyway-database-postgresql`) |
| Database | PostgreSQL 17, local via Podman (`compose.yaml`) |
| API docs | springdoc-openapi (version compatible with the chosen Boot release) |
| Ops | Spring Boot Actuator (`health` only exposed) |
| Tests | JUnit 5, AssertJ, MockMvc, Testcontainers (PostgreSQL module) |
| Formatting | Spotless with palantir-java-format (4-space indent) |
| Coverage | JaCoCo |

Exact dependency versions are pinned in `pom.xml` (managed by the Spring Boot parent where possible).

## Commands

All commands run from the `backend/` directory.

```
Start DB:     podman compose up -d
Stop DB:      podman compose down
Run app:      ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
Unit+IT test: ./mvnw test
Full check:   ./mvnw verify          # tests + coverage gate + format check
Format:       ./mvnw spotless:apply
Package:      ./mvnw clean package
```

Testcontainers with Podman requires the Podman socket. One-time setup (documented in README):

```
systemctl --user enable --now podman.socket
export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock
export TESTCONTAINERS_RYUK_DISABLED=true
```

## Project Structure

Package-by-feature. Each later module adds its own package; shared plumbing lives in `common`.

```
backend/
├── CAPABILITIES.md, SPEC-*.md        → capability map and module specs
├── README.md                          → setup and run instructions
├── compose.yaml                       → local PostgreSQL
├── pom.xml, mvnw, .mvn/               → Maven build + wrapper
└── src/
    ├── main/java/com/expensetracker/
    │   ├── ExpenseTrackerApplication.java
    │   └── common/
    │       ├── error/                 → GlobalExceptionHandler, domain exceptions
    │       └── config/                → CorsConfig, OpenApiConfig
    ├── main/resources/
    │   ├── application.yml            → shared config, reads env vars
    │   ├── application-dev.yml        → local dev defaults (matches compose.yaml)
    │   └── db/migration/              → Flyway scripts (V1__baseline.sql, ...)
    └── test/java/com/expensetracker/
        ├── support/IntegrationTest.java → base class: @SpringBootTest + Testcontainers Postgres + MockMvc
        └── common/...                 → tests for foundation behavior
```

Later modules: `com.expensetracker.identity`, `.category`, `.expense`, `.report`, `.budget` — each with `web/` (controller + DTOs), `domain/` (entity + service) and `persistence/` (repository) only when the module is large enough to warrant it; otherwise flat.

## Configuration

- `application.yml` reads everything environment-specific from env vars: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CORS_ALLOWED_ORIGINS`.
- `application-dev.yml` contains non-secret local defaults that match `compose.yaml` (local container credentials only).
- Real secrets are never committed. `.env` files are git-ignored.
- `spring.jpa.hibernate.ddl-auto=validate` — Flyway owns the schema; Hibernate never alters it.
- `spring.jpa.open-in-view=false`.

## Behavior

### Error format

All errors return `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/v1/expenses",
  "errors": [
    { "field": "amount", "message": "must be greater than 0" }
  ]
}
```

| Situation | Status | Notes |
|---|---|---|
| Bean validation failure (`@Valid` body, params) | 400 | `errors` array with `field` + `message` |
| Malformed JSON / type mismatch | 400 | no `errors` array |
| `NotFoundException` (domain) | 404 | |
| `ConflictException` (domain) | 409 | used later by category deletion |
| Unknown route | 404 | |
| Method not allowed | 405 | |
| Unhandled exception | 500 | generic `detail`; stack trace logged, never returned |

`NotFoundException` and `ConflictException` live in `common.error` so later modules reuse them.

### CORS

- Allowed origins from `CORS_ALLOWED_ORIGINS` (comma-separated); dev default `http://localhost:5173`.
- Applies to `/api/**`. Methods: `GET, POST, PUT, PATCH, DELETE, OPTIONS`. Headers: `Authorization, Content-Type`.
- When `identity` adds Spring Security, CORS must be wired through Security's `cors()` so preflight requests are not blocked by the auth filter.

### Health and docs

- `GET /actuator/health` → `200 {"status":"UP"}` when the DB is reachable. No other actuator endpoints exposed.
- `GET /v3/api-docs` → OpenAPI JSON (the contract consumed by the frontend repo).
- `GET /swagger-ui.html` → Swagger UI.

### Database

- `V1__baseline.sql` creates nothing but proves Flyway runs (e.g. a comment-only script or `CREATE EXTENSION IF NOT EXISTS pgcrypto` if needed for UUIDs).

## Code Style

- palantir-java-format (4-space indent), enforced by Spotless in `verify`.
- Base package and Maven `groupId`: `com.expensetracker`; `artifactId`: `expense-tracker-api`.
- DTOs are Java `record`s; entities are classes. Never expose entities from controllers.
- Constructor injection only, no field `@Autowired`.
- Controllers stay thin: validate, delegate to a service, map to DTO.
- Names: `XxxController`, `XxxService`, `XxxRepository`, request DTO `CreateXxxRequest`, response DTO `XxxResponse`.

```java
@RestController
@RequestMapping("/api/v1/categories")
class CategoryController {

    private final CategoryService categoryService;

    CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CategoryResponse create(@Valid @RequestBody CreateCategoryRequest request) {
        return CategoryResponse.from(categoryService.create(request.name()));
    }
}

record CreateCategoryRequest(@NotBlank @Size(max = 50) String name) {}
```

## Testing Strategy

| Level | Tool | Used for |
|---|---|---|
| Unit | JUnit 5 + AssertJ (+ Mockito sparingly) | Pure domain/service logic, no Spring context |
| Integration | `@SpringBootTest` + MockMvc + Testcontainers PostgreSQL | HTTP contract, validation, persistence, Flyway, security |

- Integration tests extend `support/IntegrationTest`, which starts one shared PostgreSQL container per test run.
- No H2 — tests run against real PostgreSQL to match production SQL behavior.
- Test names describe behavior: `returnsProblemDetailWhenRouteIsUnknown()`.
- Coverage: JaCoCo report on every `verify`; gate at **80% line coverage** on `com.expensetracker` (excluding the `Application` main class). Gate fails the build.

Foundation tests cover: health endpoint, 404 / 405 / 400 / 500 ProblemDetail shapes (via a test-only controller), CORS allowed vs. disallowed origin preflight, `/v3/api-docs` availability, Flyway migration applied.

## Boundaries

- **Always:** run `./mvnw verify` before each commit; write schema changes as new Flyway scripts; return ProblemDetail for every error; keep `tasks/` in `.gitignore`.
- **Ask first:** adding a dependency not listed in this spec; editing an already-applied Flyway migration; changing the error format or API prefix; changing coverage thresholds.
- **Never:** commit secrets or `.env` files; use `ddl-auto=update/create`; return stack traces or entities in responses; delete or `@Disabled` a failing test without approval.

## Success Criteria

1. From a clean clone with the Podman socket enabled, `./mvnw verify` passes.
2. With `podman compose up -d` and the `dev` profile, the app starts and Flyway reports `V1` applied.
3. `GET /actuator/health` returns `200` with `"status":"UP"`.
4. `GET /api/v1/does-not-exist` returns `404` with `Content-Type: application/problem+json` and the fields above.
5. A validation failure returns `400` with an `errors` array containing the failing field.
6. An unhandled exception returns `500` without a stack trace in the body.
7. A CORS preflight from `http://localhost:5173` gets `Access-Control-Allow-Origin`; one from an unlisted origin does not.
8. `GET /v3/api-docs` returns valid OpenAPI JSON; `/swagger-ui.html` loads.
9. `./mvnw spotless:check` passes; coverage gate is active.
10. `README.md` documents setup, commands and the Podman socket step.

## Resolved Decisions

1. Base package and `groupId`: `com.expensetracker`.
2. Coverage gate: 80% line coverage, fails the build.
3. Formatter: palantir-java-format (4-space indent).

## Open Questions

None.
