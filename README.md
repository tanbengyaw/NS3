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

- Staff login uses `identity.staff_user` (migration **0030**). Dev password for seeded users: `password`
- Admin user: `admin` / `password` — can manage staff via `POST /api/v1/staff-users`
- Swagger UI: `/assist-provider/swagger-ui/index.html`

### Dev database (DBeaver)

Dev uses a **file-backed H2** database at `./data/ns3_assist.mv.db` (created on first backend start). Data **persists** between restarts.

The browser H2 console does not work reliably with context path `/assist-provider` — use **DBeaver** instead:

1. Start the backend first: `.\run-dev.ps1` (must be running)
2. DBeaver → **New connection** → **H2**
3. Settings:

| Field | Value |
|--------|--------|
| **JDBC URL** | `jdbc:h2:file:C:/Users/bengy/git/NS3_new_framewrok/data/ns3_assist;MODE=PostgreSQL;AUTO_SERVER=TRUE` |
| **User** | `sa` |
| **Password** | *(leave empty)* |

Adjust the path if your repo is elsewhere. `AUTO_SERVER=TRUE` is required so DBeaver can attach while Spring Boot holds the database.

4. **Test connection** → **Finish**
5. Example query:

```sql
SELECT * FROM registration.portal_user;
```

Other schemas: `reference` (lookup tables), `registration` (cases, employers, portal users), `identity` (staff users), `base` (portal profile + documents).

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
- SST new-reg: sales tax (1100, SMK `-CJ-`) and tourism tax (1101, SMK `-CO-`)

## Pending

| Priority | Task | Notes |
|----------|------|--------|
| **Next UI** | **Angular `assist-web-app` shell** | **Started:** login, shell, employer list, registration wizards, staff admin. See [assist-web-app/README.md](./assist-web-app/README.md). |
| **Next backend** | **Extend special cases** | Port from ASSIST `SpecialCase`: **AEC** employer flag, **employee name mismatch** (temp vs live/JPN). Route non-RO submit → `IN_PROGRESS`; RO rules per case type. |
| Later | **Full national postcode master** | Dev seeds cover all wizard postcodes + area codes (0024). Production: export legacy ASSIST → CSV → Liquibase. See [docs/reference-data-load.md](./docs/reference-data-load.md). |
| Later | PostgreSQL hardening | Dev uses H2; prod profile already defined |
| Later | Directors, premises, documents | Additional Form 1 / attachments |
| Later | jBPM / full ASSIST workflow parity | Out of Phase 1 scope |
| Later | **Remaining SST tax flows** (service / digital) | Sales (1100), tourism (1101), and DPSP (1104) new-reg are done. Workshop: [docs/sst-discovery-workshop.md](./docs/sst-discovery-workshop.md) · Question bank: [docs/sst-discovery-questions.md](./docs/sst-discovery-questions.md) |
| Later | **Portal submit → branch routing** | Done — `PostcodeBranchRoutingService` on portal submit |
| Later | **Role-based submit routing** | Done — `RegistrationCaseSubmitRouter` + `RegistrationSectionRouting` (RO / UO / PKR_BO / section-aware) |

## Known limitations

### Portal employer cases route to HQ at create

~~Portal employer submissions were stuck at HQ until submit.~~ **Fixed:** on **submit**, portal cases resolve `processing_pks_branch_id` from business postcode via `reference.ref_postcode_office` (`PostcodeBranchRoutingService`). Unknown postcodes keep HQ (branch 1).

At **create**, portal cases still start at HQ; routing applies when the employer submits.

Staff-created cases (OTC) use the officer's `branch_id` at create and are unchanged.

### Submit routing by role and section

On **submit**, `RegistrationCaseSubmitRouter` sets the next status from the submitter's role and case section:

- **RO** — auto-approves new registration (section 200, 1100–1102, 1104); update/discontinue tax (1200–1204, 1103) goes to **SUBMITTED** (UO queue).
- **UO** — update/discontinue tax → **SUBMITTED**; incomplete OTC flag → **IN_PROGRESS**.
- **PKR_BO** — incomplete flag or incomplete tax sections (1205–1209) → **APPROVED**.
- **Other staff** — incomplete tax sections (1205–1209) auto-approve; OTC incomplete flag → **IN_PROGRESS**.
- **Employer (portal)** — **SUBMITTED** (special cases → **IN_PROGRESS**).

Staff can pass `{ "incomplete": true }` on submit for OTC incomplete handling. The registration wizard shows role-specific hints and the incomplete checkbox for counter staff.

Full reference: [docs/submit-routing.md](./docs/submit-routing.md)
