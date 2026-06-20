# AGENTS.md

## Repository context
- No prior AI instruction files were found via the requested glob search (`README.md`, `AGENTS.md`, `.github/copilot-instructions.md`, etc.).
- This repo is the **Core Spendoo service** from the ADR (`docs/Spendoo Backend ADR.md`): a modular monolith intended to keep financial workflows cohesive.

## Architecture that matters first
- Runtime entrypoint is `app` (`app/src/main/kotlin/org/spendoo/app/SpendooApplication.kt`), which scans `org.spendoo` and enables async + scheduling globally.
- Modules are Gradle subprojects: `:app`, `:identity`, `:transactions`, `:events`, `:notifications`, `:storage` (`settings.gradle.kts`).
- `:app` depends on all feature modules and is the deployable unit (`app/build.gradle.kts`, `Dockerfile`).
- `:events` is the internal contract bus: marker interface `SpendooEvent`, publisher abstraction `SpendooEventPublisher`, Spring-backed impl `SpendooEventPublisherImp`.
- Cross-module flow examples:
  - Identity verification publishes `UserCreatedEvent` -> Transactions listener inserts default categories (`identity/service/AuthService.kt`, `transactions/eventListener/UserCreatedListener.kt`, `transactions/repository/CategoryRepository.kt`).
  - Identity email actions publish `EmailEvent` -> Notifications async listener sends SMTP mail (`identity/service/EmailService.kt`, `notifications/AsyncEmailListener.kt`).
  - Identity profile image updates call S3 storage service and then publish `UserUpdatedEvent` (`identity/service/UserService.kt`, `storage/service/ImageStorageService.kt`).

## Build, test, run workflows
- Verified project graph command:
```powershell
.\gradlew.bat -q projects
```
- Build all modules:
```powershell
.\gradlew.bat build
```
- Run app module locally:
```powershell
.\gradlew.bat :app:bootRun
```
- Run focused tests (example from attached integration suite):
```powershell
.\gradlew.bat :transactions:test --tests "*TransactionServiceIntegrationTest"
```
- Container build path uses multi-stage Docker and packages `:app:bootJar` with tests skipped (`Dockerfile`).

## Project-specific conventions
- API routes are versioned under `/api/v1/...` (see `identity/api/controller/*`, `transactions/api/controller/*`).
- Security principal is a **UUID userId** set directly by `JwtFilter`; controllers consume it via `@AuthenticationPrincipal userId: UUID`.
- Auth/public path exemptions are duplicated in both `SecurityConfig` and `JwtFilter`; update both when changing access rules.
- Integration tests typically boot module-specific test configs (`IdentityTestApplication`, `TransactionsTestApplication`) with `@ActiveProfiles("test")` and H2.
- Identity tests mock cross-module services/events (e.g., `SpendooEventPublisher`, `ImageStorageService`) to isolate behavior.
- Transactions default category bootstrap uses native SQL against `spending.categories` + `gen_random_uuid()`; keep Postgres compatibility in mind.
- Every class or interface MUST reside in its own separate file (do not group multiple classes/DTOs in one file).
- Use type-safe Enums instead of raw Strings for status levels, directions, or trends.
- Database queries must be memory-safe: use JPQL aggregations (e.g. `SUM`, `COUNT`) or lightweight DTO projections rather than fetching entire entity lists/graphs.
- **CRITICAL**: Do NOT make queries with `getAll` (or `findAll`) without using `Pageable` pagination, because fetching all records into memory will cause OutOfMemory errors on the limited server memory.
- Keep business logic decoupled: isolate static configurations, color palettes, and calendar/date math into distinct files/utilities.
- Centralize localization: use a centralized translation registry rather than inline language check branching.
- Do not write comments in code: write clean, readable, self-explanatory code instead.
- Use MockK instead of Mockito for all testing/mocking needs across the codebase.
- Do not use fully qualified class names; always import them instead.

## External dependencies and config touchpoints
- Prod DB is PostgreSQL; tests use H2 in PostgreSQL mode (`*/src/test/resources/application-test.properties`).
- Required runtime env-backed properties are in `app/src/main/resources/application.properties`: DB creds, mail creds, `JWT_SECRET`, and `storage.spendoo.*`.
- Storage uses AWS SDK v2 S3 client with custom endpoint/path-style config (DigitalOcean Spaces style) from `app/config/storage/StorageConfig.kt`.
- Scheduled cleanup jobs run daily in identity auth service (`AuthService.clearExpired*`, `clearUnverifiedUsers`).

## Safe change strategy for agents
- Prefer implementing logic in the owning module; keep `:app` focused on composition/wiring.
- For new domain events: add contract in `:events`, publish through `SpendooEventPublisher`, consume with `@EventListener` (often `@Async`), then update module tests.
- When adding secured endpoints, preserve the UUID principal pattern instead of introducing custom principal DTOs unless refactoring auth globally.
- Keep package roots under `org.spendoo` so existing component/entity/repository scanning continues to work.

