# Admin & staff menu scope (Phase 1)

**Status:** Active — defines what NS3 **includes** vs legacy ASSIST BASE menus **not** required for Phase 1.

**Related:**

| Item | Location |
|------|----------|
| Sidebar implementation | `assist-web-app/src/app/layout/shell.component.html` |
| Role-based home redirect | `assist-web-app/src/app/core/auth/role-home-redirect.component.ts` |
| Full legacy BPM / inbox parity | [legacy-bpm-parity-plan.md](./legacy-bpm-parity-plan.md) (out of Phase 1 scope) |

---

## Summary

Phase 1 does **not** replicate the full legacy **BASE** module navigation (My Task, Workflow console, Maintenance, Quartz, etc.). Staff and admin users get the menus needed to run **portal enrollment**, **SST registration**, and **case routing** for the current vertical slice.

If a legacy BASE screen is missing from the table below, assume **not in Phase 1 scope** unless product explicitly requests it.

---

## Who sees what

| Role | Home | Navigation |
|------|------|------------|
| **Portal employer** | Tax Registrant 360 | Base → Tax Registrant 360; Registration → SST wizards only |
| **Staff (officer / RO / UO / etc.)** | Employers list | Base → Portal ID registration, Case routing listing; Registration → SST wizards, Update/Discontinue tax, Officer inbox |
| **Admin** | Employers list | Same as staff, plus Administration → Staff users |

Employers do **not** see staff-only items (employer list, portal enrollment admin, officer inbox, update/discontinue tax). Staff do **not** see employer-only Tax Registrant 360 in the sidebar (they use Employers and registration tools instead).

---

## In scope today (NS3 sidebar)

### Base (staff only)

| Menu item | Route | Purpose |
|-----------|-------|---------|
| Portal ID registration | `/portal-id-registration` | Enroll portal users; approve/reject and activate login |
| Case routing listing | `/base/case-routing` | View case routing / branch assignment |

### Base (portal employer only)

| Menu item | Route | Purpose |
|-----------|-------|---------|
| Tax Registrant 360 | `/base/tax-registrant` | Notifications + registration summary (Phase 1: 2 tabs) |

### Registration (all authenticated users)

| Menu item | Route | Notes |
|-----------|-------|-------|
| Sales / Tourism / DPSP / Service / Digital tax registration | `/registration/*` | SST new-registration wizards |
| Update tax payer | `/registration/update-tax-payer` | Staff only |
| Discontinue tax | `/registration/discontinue-tax` | Staff only |
| Officer inbox | `/registration/inbox` | Staff only |

SOCSO new registration is **hidden** from the nav; existing `/registration` routes still work for legacy cases.

### Administration (admin only)

| Menu item | Route | Purpose |
|-----------|-------|---------|
| Staff users | `/admin/staff-users` | Create and manage staff accounts |

---

## Legacy BASE menus — not required for Phase 1

These exist in legacy ASSIST BASE but are **not** planned for the Phase 1 NS3 sidebar. Workflow behavior that matters for registration is implemented in code ([submit-routing.md](./submit-routing.md)), not via a BPM task UI.

| Legacy area (examples) | Why omitted in Phase 1 |
|------------------------|-------------------------|
| My Task / workflow task inbox (jBPM) | No BPM engine; staff use **Officer inbox** (status-filtered cases) |
| Workflow maintenance / process admin | Out of scope — see [legacy-bpm-parity-plan.md](./legacy-bpm-parity-plan.md) |
| Reference-data maintenance UI (codes, postcodes, etc.) | Loaded via Liquibase/seeds; UI deferred unless business requires live edits |
| Quartz / batch job console | No batch scheduler UI in NS3 yet |
| System parameters / global config screens | Spring config + migrations; no admin UI |
| Audit / document / BOD legacy BASE tabs | Partially covered by Tax Registrant 360 for employers; full parity deferred |

---

## When to extend

Add a menu item when **all** of the following apply:

1. Backend API and permissions exist (or are in the same delivery).
2. The screen supports a Phase 1 or agreed Phase 2 user story (not “parity for parity’s sake”).
3. The role that needs it is defined (staff vs employer vs admin).

For full legacy BASE or BPM parity across modules, use [legacy-bpm-parity-plan.md](./legacy-bpm-parity-plan.md) as the architecture reference — separate from day-to-day Phase 1 menu decisions.
