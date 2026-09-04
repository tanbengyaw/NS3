# NS3 ASSIST — Fineract-style platform (employer registration slice)

PERKESO ASSIST replatform following [Apache Fineract](https://github.com/apache/fineract) architecture patterns.
UI reference: [Mifos web-app](https://github.com/openMF/web-app) (separate `assist-web-app` folder).

## Modules

| Module | Role |
|--------|------|
| `assist-core` | Command pipeline, shared DTOs, pagination |
| `assist-registration` | Employer domain, REST API, Liquibase |
| `assist-identity` | Portal enrollment (new / existing employer) |
| `assist-provider` | Spring Boot runnable application |
| `assist-web-app` | Angular UI (scaffold — not started) |

## Prerequisites

- **Java 17+** for this project only (you already have `C:\Program Files\Java\jdk-17`; global JAVA_HOME can stay on Java 11 for legacy ASSIST)
- PostgreSQL 14+ for production profile, or use `dev` profile with embedded H2

## Quick start (Windows — recommended)

Uses Java 17+ **only in this terminal**; does not change global `JAVA_HOME` for `assist_testing_only`.

```powershell
cd C:\Users\bengy\git\NS3_new_framewrok
.\run-dev.ps1
```

The script auto-detects `C:\Program Files\Java\jdk-17` if present. Optional: install JDK 21 later via winget.

Build and run all tests:

```powershell
.\run-build.ps1
```

Optional custom JDK path:

```powershell
[System.Environment]::SetEnvironmentVariable("NS3_JAVA_HOME", "C:\Program Files\Java\jdk-17", "User")
```

## Quick start (manual Gradle)

Only if `JAVA_HOME` already points to Java 17+ in that session:

```bash
./gradlew :assist-provider:bootRun -Dspring.profiles.active=dev
```

Production (PostgreSQL):

```bash
createdb ns3_assist
./gradlew :assist-provider:bootRun
```

Default API (dev): `http://localhost:8081/assist-provider/api/v1/employers`

- User: `admin` / Password: `password`
- Swagger UI: `/assist-provider/swagger-ui/index.html`

## Architecture

```
REST (*ApiResource, Jersey)
  → CommandWrapperBuilder → CommandProcessingService
  → @CommandType handler → *WritePlatformService (JPA)
  → *ReadPlatformService (JdbcTemplate)
```

Phase 1 vertical slice: **employer registration** (search, registration cases, approve → employer code).

## Completed (backend Phase 1)

- Registration case CRUD, submit / approve / reject / query (`IN_QUERY`)
- Portal vs OTC routing, RO auto-approve, officer incomplete → `IN_PROGRESS`
- Form 2 temp employees, portal user link on approve, employee promotion
- Special case: duplicate BRN → non-RO `IN_PROGRESS`, RO `REJECTED`
- `assist-identity`: portal enrollment (`NEW_EMPLOYER`, `EXISTING_EMPLOYER`)
- E2E integration tests (`RegistrationWorkflowE2EIT`)

## Pending

| Priority | Task | Notes |
|----------|------|--------|
| **Next UI** | **Angular `assist-web-app` shell** | **Started:** login, shell, employer list. Next: registration wizard, officer inbox. See [assist-web-app/README.md](./assist-web-app/README.md). |
| **Next backend** | **Extend special cases** | Port from ASSIST `SpecialCase`: **AEC** employer flag, **employee name mismatch** (temp vs live/JPN). Route non-RO submit → `IN_PROGRESS`; RO rules per case type. |
| Later | **Full national postcode master** | Dev seeds cover all wizard postcodes + area codes (0024). Production: export legacy ASSIST → CSV → Liquibase. See [docs/reference-data-load.md](./docs/reference-data-load.md). |
| Later | PostgreSQL hardening | Dev uses H2; prod profile already defined |
| Later | Directors, premises, documents | Additional Form 1 / attachments |
| Later | jBPM / full ASSIST workflow parity | Out of Phase 1 scope |
| Later | **SST tax flows** (sales / service / tourism / digital / DPSP) | **Workshop first:** [docs/sst-discovery-workshop.md](./docs/sst-discovery-workshop.md) · Question bank: [docs/sst-discovery-questions.md](./docs/sst-discovery-questions.md) |
