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

## Pending

- [ ] Employer detail view
- [ ] **Officer inbox** — list pending cases (needs list API)
- [ ] **Portal flows** — enrollment (new vs existing employer), resubmit after `IN_QUERY`

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

| User | Password | Role |
|------|----------|------|
| `admin` | `password` | Officer / admin |
| `ro` | `password` | Registration officer (auto-approve on submit) |
| `employer` | `password` | Portal employer |

Header: `Authorization: Basic …` (via HTTP interceptor)

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
