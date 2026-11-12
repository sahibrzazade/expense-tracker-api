# expense-tracker-api

REST API for a personal expense tracker, built with Spring Boot and PostgreSQL.

- Module map and cross-cutting decisions: [CAPABILITIES.md](CAPABILITIES.md)
- Module specs: `SPEC-<module>.md` (e.g. [SPEC-foundation.md](SPEC-foundation.md))

## Prerequisites

| Tool | Version | Check |
|---|---|---|
| JDK (not just a JRE) | 25 | `javac -version` |
| Docker Engine + Compose plugin | recent | `docker compose version` |

Maven is **not** required: the Maven Wrapper (`./mvnw`) downloads the right version on first use.

Your user must be able to run `docker` without `sudo` (Linux: `sudo usermod -aG docker $USER`, then log out and back in). Integration tests use Testcontainers, which talks to the same Docker daemon.

## Getting started

```bash
# 1. Start the local PostgreSQL (data persists in a Docker volume)
docker compose up -d

# 2. Run the app with the "dev" profile (local DB settings from application-dev.yml)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Check it is up
curl localhost:8080/actuator/health   # {"status":"UP"}
```

In IntelliJ IDEA: open the `backend` folder, load the Maven project when prompted, then set **Active profiles** to `dev` in the `ExpenseTrackerApplication` run configuration.

## Commands

| Task | Command |
|---|---|
| Start / stop the dev database | `docker compose up -d` / `docker compose down` |
| Reset the dev database (deletes data) | `docker compose down -v` |
| Run the app | `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` |
| Run tests | `./mvnw test` |
| Full check (tests, format, coverage gate) | `./mvnw verify` |
| Format code | `./mvnw spotless:apply` |
| Build an executable jar | `./mvnw clean package` |

Run `./mvnw spotless:apply` before committing; `verify` fails on unformatted code and on line coverage below 80%. The coverage report is written to `target/site/jacoco/index.html`.

Tests do **not** need the dev database: each run starts a throwaway PostgreSQL container via Testcontainers.

## Configuration

The base config (`application.yml`) reads environment-specific values from environment variables:

| Variable | Purpose |
|---|---|
| `DB_URL` | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/expense_tracker` |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |

The `dev` profile (`application-dev.yml`) provides local defaults matching `compose.yaml`; those credentials are for the local container only. Never commit real secrets.

## Project layout

```
src/main/java/com/expensetracker/   application code, one package per module
src/main/resources/
  application.yml                   base config
  application-dev.yml               local dev profile
  db/migration/                     Flyway migrations (V1__..., V2__...)
src/test/java/com/expensetracker/
  support/                          IntegrationTest base class + Testcontainers config
compose.yaml                        local PostgreSQL
```

Schema changes are always new Flyway scripts; never edit a migration that has already been applied.

## Endpoints

| Path | Description |
|---|---|
| `GET /actuator/health` | Health check (includes database connectivity) |
