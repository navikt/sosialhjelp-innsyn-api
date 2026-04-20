# Copilot Instructions: sosialhjelp-innsyn-api

Backend API (Spring Boot WebFlux + Kotlin) that lets Norwegian citizens view their own social assistance (sosialhjelp) cases. Part of **team digisos** at NAV. Deployed on the [NAIS](https://doc.nais.io) platform (GCP).

## Build, test, and lint

```bash
# Build (also installs the pre-push git hook for spotless)
./gradlew build

# Run all tests
./gradlew test

# Run a single test class or method
./gradlew test --tests "no.nav.sosialhjelp.innsyn.vedlegg.VedleggServiceTest"
./gradlew test --tests "no.nav.sosialhjelp.innsyn.vedlegg.VedleggServiceTest.some test name"

# Check formatting
./gradlew spotlessCheck

# Auto-fix formatting (run before committing)
./gradlew spotlessApply
```

> **GitHub Packages**: The build fetches internal NAV libraries from GitHub Package Registry. Requires `githubUser` and `githubPassword` in `~/.gradle/gradle.properties`.

## Architecture

The app is a **stateless REST API** — no persistent database. Data lives in external systems fetched on each request; Valkey (Redis-compatible) is used only for caching.

### Core data flow

Most endpoints follow this pattern:
1. `FiksService.getSoknad(fiksDigisosId)` — fetches the `DigisosSak` object from KS FIKS
2. `EventService.createModel(digisosSak)` — builds an `InternalDigisosSoker` by replaying the JSON event log (`JsonDigisosSoker`) from FIKS
3. The controller maps the model to a response DTO

Each file under `event/` (e.g. `SoknadsStatus.kt`, `Utbetaling.kt`) handles one event type and mutates the model during replay.

### Package structure

Code is organized by feature under `no.nav.sosialhjelp.innsyn`:

| Package | Responsibility |
|---|---|
| `app/` | Cross-cutting: security config, HTTP client config, exception handling, feature toggles (Unleash), token utilities, Texas token-exchange client, MDC/tracing filters |
| `digisosapi/` | FIKS integration: `FiksService` (caching layer) + `FiksClient` (HTTP), plus test-only endpoints |
| `digisossak/` | Domain features per sub-area: `sak/`, `soknadsstatus/`, `saksstatus/`, `utbetalinger/`, `hendelser/`, `oppgaver/`, `brev/`, `forelopigsvar/` |
| `event/` | Event-sourcing replay — each file processes one JSON event type into `InternalDigisosSoker` |
| `domain/` | Internal domain model (`InternalDigisosSoker` and related classes) |
| `klage/` | Complaint submission — generates PDF/JSON, uploads via FIKS Klage API |
| `kommuneinfo/` | Municipality capability checks (e.g. whether a municipality accepts digital submissions) |
| `tilgang/` | Access control — blocks kode 6/7 protected persons via PDL lookup |
| `pdl/` | PDL (Folkeregisteret) GraphQL client |
| `navenhet/` | NAV unit lookup via NORG |
| `saksoversikt/` | Application list endpoint |
| `valkey/` | Valkey/Redis cache configuration and cache key definitions |
| `vedlegg/` | Attachment upload (multipart, virus scan, encryption, PDF cover sheet) and download |
| `utils/` | Shared utilities: `logger()` delegate, `sosialhjelpJsonMapper`, date/string helpers |

### External integrations

| System | Purpose | Key config |
|---|---|---|
| **KS FIKS** | Main data source — all case data, documents, status | `FIKS_DIGISOS_ENDPOINT_URL`, `INTEGRASJONSID_FIKS`, `INTEGRASJONPASSORD_FIKS` |
| **PDL** | Person data (name lookup, kode 6/7 check) | `PDL_ENDPOINT_URL`, `PDL_AUDIENCE` |
| **NORG** | NAV unit names | `NORG_URL` |
| **ClamAV** | Virus scanning for uploaded files | `CLAMAV_URL` |
| **Unleash** | Feature toggles | `UNLEASH_SERVER_API_URL`, `UNLEASH_SERVER_API_TOKEN` |
| **Texas** | NAIS token exchange for service-to-service calls | `NAIS_TOKEN_EXCHANGE_ENDPOINT` |
| **ID-porten** | Citizen authentication (JWT) | `IDPORTEN_ISSUER`, `IDPORTEN_CLIENT_ID` |

## Key conventions

### Controllers always check access first

Every controller method starts with `tilgangskontroll.sjekkTilgang()` before doing any work. This rejects kode 6/7 users.

### All controller and service methods are `suspend`

The app is fully reactive (Spring WebFlux). Use `suspend` functions; use `coEvery`/`coVerify` in tests for coroutine-aware mocking. Reactor types (`Mono`, `Flux`) appear in the HTTP client layer but are converted to coroutines with `awaitSingle()` / `awaitSingleOrNull()` / `.asFlow()` at the boundary.

### Authentication

Security is configured in `SecurityConfig` with `@Profile("!test")`. JWT tokens from ID-porten are validated for:
- **Audience**: `client_id` claim must match `IDPORTEN_CLIENT_ID`
- **ACR**: must be `Level4` or `idporten-loa-high`

Public paths: `/internal/**` and `/v3/api-docs`.

### Logging

Use the `logger()` extension delegate, always inside a `companion object`:

```kotlin
companion object {
    private val log by logger()
}
```

Never log personal identifiers (fnr/dnr). Use `messageUtenFnr()` / `toFiksErrorMessageUtenFnr()` utilities when logging Fiks errors.

### JSON

Use `sosialhjelpJsonMapper` (defined in `utils/`) — not a freshly instantiated ObjectMapper — for consistency with the shared configuration used across the codebase.

### Linting

Spotless + ktlint is enforced. A pre-push git hook is installed automatically when running `./gradlew build`. Running `./gradlew spotlessApply` before committing avoids hook failures.

Mockito is **excluded** from the build. Use MockK only.

### Testing patterns

**Unit tests** — plain MockK, no Spring context:

```kotlin
internal class MyServiceTest {
    private val dep: MyDep = mockk()
    private val service = MyService(dep)

    @BeforeEach
    fun setUp() { clearAllMocks() }

    @Test
    fun `some behaviour`() = runTest { ... }
}
```

**Integration tests** — extend `AbstractIntegrationTest`, which boots the full application with profiles `mock-redis,test,local_unleash`, issues real JWT tokens via `MockOAuth2Server`, and exposes a `WebTestClient`. Use `@MockkBean` to replace Spring beans:

```kotlin
class MyIntegrationTest : AbstractIntegrationTest() {
    @MockkBean lateinit var fiksService: FiksService

    @Test
    @WithMockUser
    fun `test endpoint`() {
        coEvery { fiksService.getAllSoknader() } returns listOf(...)
        doGet("/api/v1/innsyn/saker", emptyList()).expectStatus().isOk
    }
}
```

### Local development profiles

| Profile combination | Use case |
|---|---|
| `mock-alt,log-console` | Run `Application` — no real FIKS/PDL, all stubbed via mock-alt |
| `local,log-console` | Run `TestApplication` — real FIKS integration; needs `INTEGRASJONPASSORD_FIKS`, `INTEGRASJONSID_FIKS`, `TESTBRUKER_NATALIE` env vars |
| Add `mock-redis` | Skip real Valkey/Redis when running locally |

Base URL path in all environments: `/sosialhjelp/innsyn-api`. API endpoints are under `/api/v1/innsyn/`.
