# NS3 ASSIST Web App (Angular)

UI shell following [Mifos web-app](https://github.com/openMF/web-app) patterns.

**Status:** Initial shell — login, layout, employer list, **registration wizard**.

## Done

- [x] Angular 19 project initialized
- [x] Core: API base URL, HTTP Basic auth interceptor, route guard
- [x] Login page (dev users)
- [x] Shell layout (sidebar + content)
- [x] **Employers** — search list (name / code / BRN)
- [x] **Registration wizard** — Form 1, Form 2 employees, submit; officer approve on step 3
- [x] **Sales tax wizard** — Form 1, Form 2, preview & submit (section 1100)
- [x] **Tourism tax wizard** — shared SST new-reg shell, Form 1 Part A, Part B dates, preview & submit (section 1101, SMK `-CO-`)
- [x] **DPSP tax wizard** — shared SST new-reg shell, website + email, Part B dates, preview & submit (section 1104, SMK `-CT-`)
- [x] **Submit routing UI** — role-specific hints + incomplete OTC checkbox on SOCSO and Sales tax submit steps (see [docs/submit-routing.md](../docs/submit-routing.md))
- [x] **OTC incomplete loop (Sales tax)** — incomplete → `IN_PROGRESS` → resubmit → `SUBMITTED` → approve + letter (manual E2E verified)
- [x] **Staff admin UI** — list / create / edit staff users (`/admin/staff-users`, ADMIN only)
- [x] **Officer inbox** — list cases filtered by staff branch (`processing_pks_branch_id`)
- [x] **UO inbox section filter** — filter UO queue by sections 1103, 1200–1204
- [x] **Employer detail view** — `/employers/:id` profile from list click
- [x] **Inbox case review** — open case from Officer/UO inbox, approve / query / reject

## Pending
- [x] **Portal enrollment UI** — `/portal-id-registration` (new vs existing taxpayer, document upload, success summary)
- [x] **Portal resubmit** — manage tab: officer query → applicant resubmit after `IN_QUERY`
- [x] **Portal submit → branch routing** — portal cases routed to branch inbox by postcode on submit

## Setup

Backend must be running with `dev` profile (CORS enabled for `http://localhost:4200`):

```powershell
cd C:\Users\bengy\git\NS3_new_framewrok
.\run-dev.ps1
```

In another terminal:

```powershell
cd C:\Users\bengy\git\NS3_new_framewrok\assist-web-app
npm install
npm start
```

Open `http://localhost:4200` — sign in as `admin` / `password`.

`npm start` proxies `/assist-provider/api` → `http://localhost:8081` (no CORS needed in dev).

## API base URL (dev)

```
http://localhost:8081/assist-provider/api/v1
```

Configured in `src/environments/environment.ts`.

## Auth (dev)

| User | Password | Role | Branch (`ref_branch.id`) |
|------|----------|------|--------------------------|
| `admin` | `password` | Admin / officer | 2 — Shah Alam |
| `ro` | `password` | RO (auto-approve on submit) | 2 — Shah Alam |
| `uo_jb` | `password` | UO | 4 — Johor Bahru |
| `pkrbo_kl` | `password` | PKR BO (auto-approve incomplete submit) | 1 — Kuala Lumpur |
| `officer_pj` | `password` | Officer | 3 — Petaling Jaya |
| `employer` | `password` | Portal employer | — (cases route to HQ) |

Header: `Authorization: Basic …` (via HTTP interceptor)

Staff accounts are stored in `identity.staff_user` (Liquibase **0030**). Only `admin` can call `/v1/staff-users` to create or update staff.

**Staff admin UI:** sign in as `admin` → sidebar **Administration → Staff users** (`/admin/staff-users`).

Example create staff user (API):

```http
POST /assist-provider/api/v1/staff-users
Authorization: Basic admin:password
Content-Type: application/json

{
  "username": "ro_kl",
  "email": "ro.kl@perkeso.example",
  "password": "password",
  "branchId": 1,
  "roles": ["RO"]
}
```

## Project structure

```
src/app/
├── core/
│   ├── auth/           login, interceptor, guard, auth service
│   └── models/
├── layout/             shell (sidebar)
└── employers/          list + service
```

## Related backend

Registration and identity APIs in `assist-registration` and `assist-identity`. See root [README.md](../README.md).
