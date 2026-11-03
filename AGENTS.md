# AGENTS.md

Instructions for AI coding agents working in this repository.

## Stack

- Spring Boot **4.1.2**, Gradle wrapper 9.8.2, Java 21 toolchain, single module (`financial-api`), base package `com.agavide.financialapi`.
- Spring Boot 4 starter names differ from Boot 3: web is `org.springframework.boot:spring-boot-starter-webmvc` (not `-starter-web`), and test deps are modular (e.g. `spring-boot-starter-data-jpa-test`, not `-starter-test`). The classic names will not resolve.

## Commands

- Build/verify: `./gradlew build`
- All tests: `./gradlew test`
- Single test class: `./gradlew test --tests 'com.agavide.financialapi.FinancialApiApplicationTests'`
- There is no lint, formatter, or CI config; Gradle test is the verification step.

## Tests

- Tests get their Postgres through Testcontainers (`@ServiceConnection` on a `PostgreSQLContainer` in `FinancialApiApplicationTests`). **Docker must be running** or every `@SpringBootTest` fails with a container-startup error.
- Test datasource is not read from `application.yaml`; do not "fix" test failures by pointing at the real database.

## Runtime config

- `src/main/resources/application.yaml` reads `POSTGRES_URL` / `POSTGRES_USER` / `POSTGRES_PASSWORD` env vars (URL defaults to `jdbc:postgresql://localhost:5432/financial`). The app won't start without user/password set.
- Never commit connection credentials; use environment variables for environment-specific values.

## Dependency gotchas (learned the hard way)

- Testcontainers is **2.x**, managed by Boot's BOM. Artifact names differ from 1.x docs: `org.testcontainers:testcontainers-postgresql` (not `:postgresql`), and JUnit annotations need `org.testcontainers:testcontainers-junit-jupiter`.
- TC 2.x packages moved: use `org.testcontainers.postgresql.PostgreSQLContainer` and `org.testcontainers.junit.jupiter.{Container,Testcontainers}`. Container classes are **no longer generic** — write `PostgreSQLContainer`, not `PostgreSQLContainer<?>`.

## API docs

- springdoc-openapi 3.2.0 is on the classpath; controllers should keep OpenAPI annotations in sync with the response models.
