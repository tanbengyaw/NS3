# Submit routing & incomplete OTC flag

How registration cases move to the next status when a user clicks **Submit** (SOCSO new reg, Sales tax, and other sections).

Backend: `RegistrationCaseSubmitRouter` + `RegistrationSectionRouting`  
API: `POST /registration-cases/{id}?command=submit` with optional body `{ "incomplete": true }`

---

## UI (Angular)

Both wizards show the same controls on the final step:

| Wizard | Step | Path |
|--------|------|------|
| SOCSO new registration | 3. Submit | `/registration/:caseId` |
| Sales tax new registration | 3. Preview & route | `/registration/sales-tax/:caseId` |

On the submit step:

1. **Role-specific hint** — explains what will happen for the logged-in user and section.
2. **“Mark as incomplete OTC submission”** checkbox — shown only for counter staff (`OFFICER`, `PKR_BO`, `UO`, `ADMIN`), and only **before** the first submit while the case is still editable (`NEW`, `IN_QUERY`, or `IN_PROGRESS`).

After submit, the checkbox is hidden; use **Approve / Send to query / Reject** for cases already in the workflow.

---

## Incomplete checkbox

Use when counter staff save a **partial** OTC case and want to finish it later.

| Role | With `{ incomplete: true }` | Complete submit (no flag) |
|------|----------------------------|---------------------------|
| **OFFICER / ADMIN** | `IN_PROGRESS` — stays with counter | `SUBMITTED` |
| **UO** | `IN_PROGRESS` | `SUBMITTED` (or UO queue for update/discontinue tax) |
| **PKR_BO** | `APPROVED` — auto-approve incomplete | `SUBMITTED` |
| **RO** | Ignored — RO still auto-approves new reg | `APPROVED` (new reg only) |
| **Employer (portal)** | N/A — no checkbox | `SUBMITTED` |

**Typical OTC flow (OFFICER):**

1. Create draft at counter → fill Form 1 / Form 2 → Preview.
2. Tick **Mark as incomplete OTC submission** → Submit → **`IN_PROGRESS`**.
3. Case appears in **Officer inbox** (filtered by staff branch).
4. Complete missing data → submit again **without** the checkbox → **`SUBMITTED`**.
5. Another officer (or same user with approve rights) **Approve** → employer code + sales tax acknowledgement letter.

**Verified (2026-09-05):**

| Step | User | Result |
|------|------|--------|
| Incomplete submit | `officer_pj` + Sales tax §1100 + `{ incomplete: true }` | `IN_PROGRESS` at branch Petaling Jaya (3) |
| Complete resubmit | Same case, checkbox off | `SUBMITTED` |
| Approve | `admin` or `ro` | `APPROVED`, employer code, acknowledgement letter |

---

## Role × section matrix (submit only)

Section groups (ASSIST section IDs):

| Group | Sections | Meaning |
|-------|----------|---------|
| New reg (SOCSO) | 200 | Standard employer registration |
| New reg (SST) | 1100–1102, 1104 | Sales / service / tourism / digital / DPSP new reg |
| Discontinue tax | 1103 | SST discontinue |
| Update tax | 1200–1204 | Update taxpayer per tax type |
| Incomplete tax | 1205–1209 | Complete audit auto-reg SST data |

### RO

- **New reg** (200, 1100–1102, 1104) → **`APPROVED`** (auto-approve, promote employer).
- **Update / discontinue tax** (1200–1204, 1103) → **`SUBMITTED`** (UO queue; RO does not auto-approve).
- Duplicate BRN on new reg → **`REJECTED`** (RO only).

### UO

- **Update / discontinue tax** → **`SUBMITTED`**.
- **Incomplete OTC flag** → **`IN_PROGRESS`**.
- Other complete staff submits → **`SUBMITTED`**.

### PKR_BO

- **Incomplete flag** or **incomplete tax section** (1205–1209) → **`APPROVED`**.
- Complete new reg without incomplete → **`SUBMITTED`**.

### OFFICER / ADMIN

- **Incomplete tax section** (1205–1209) → **`APPROVED`** (legacy `IncompleteAutoRegTaxCounter`).
- **Incomplete OTC flag** → **`IN_PROGRESS`**.
- **Special cases** (e.g. duplicate BRN) → **`IN_PROGRESS`**.
- Otherwise → **`SUBMITTED`**.

### Employer (portal)

- **`SUBMITTED`** (postcode branch routing applied on submit).
- Special cases → **`IN_PROGRESS`**.

---

## Dev users for manual testing

| User | Role | Branch | Try this |
|------|------|--------|----------|
| `ro` | RO | Shah Alam (2) | New reg / sales tax → auto **APPROVED** |
| `officer_pj` | OFFICER | Petaling Jaya (3) | Incomplete checkbox → **IN_PROGRESS**; complete submit → **SUBMITTED** |
| `pkrbo_kl` | PKR_BO | KL (1) | Incomplete checkbox → **APPROVED** |
| `uo_jb` | UO | Johor Bahru (4) | Create case with `sectionId: 1201`, submit → **SUBMITTED** |
| `employer` | EMPLOYER | — | Portal submit → **SUBMITTED** + branch routing |

Password for all: `password`

---

## API example

```http
POST /assist-provider/api/v1/registration-cases/5?command=submit
Authorization: Basic officer_pj:password
Content-Type: application/json

{ "incomplete": true }
```

Response includes `changes.appStatus` (`IN_PROGRESS`, `SUBMITTED`, or `APPROVED`).

---

## Tests

| Test | Scope |
|------|--------|
| `RegistrationCaseSubmitRouterTest` | Unit — role + section combinations |
| `RoleBasedSubmitRoutingIT` | Integration — RO update tax, UO update tax, PKR_BO incomplete, officer incomplete tax section |
| `UoInboxSectionFilterIT` | Integration — UO inbox `sectionIds` filter (1103, 1200–1204) |
| `RegistrationWorkflowE2EIT` | E2E — portal, RO auto-approve, admin incomplete OTC |

Run (Java 17):

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
cd C:\Users\bengy\git\NS3_new_framewrok
.\gradlew :assist-provider:test --tests "my.gov.perkeso.assist.integration.RoleBasedSubmitRoutingIT"
```

---

## Related

- Portal submit → branch routing: `PostcodeBranchRoutingService` (postcode on submit).
- Staff inbox by branch: `EmployerReadPlatformService.retrieveCaseSummaries`.
- UO inbox section filter: `sectionIds=1103,1200,1201,1202,1203,1204` (or single `sectionId`) on `GET /registration-cases`.
- SST discovery (legacy behaviour): [sst-discovery-questions.md](./sst-discovery-questions.md) §5.9.
