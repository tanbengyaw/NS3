# Audit Create Case through taxpayer response

Staff-only Audit path. **Create Case** is still the only step that plants incomplete auto-reg. Later tabs do not call ingest.

Out of this slice: BOD screen, taxpayer portal, Camunda, ledger, existing-taxpayer search, hiding Simulate ingest.

## Officer path

1. Login as `officer_pj` / `password` (or `admin`).
2. **Audit → Audit cases → New case**.
3. Fill taxpayer (name, **new** BRN, postcode, branch) and tax-type popup fields.
4. **Create case** → Fieldwork (1101), planning seeded, case type defaults to **Field**.
5. Planning → Fieldwork → Working papers → Findings. Submit findings (1102). Approve (1103) seeds a `taxpaper_response` row (`TXR…`).
6. **Taxpayer** tab (staff records the reply; no portal yet):
   - Channel: Portal / OTC / Email. Type: Accept / Dispute / Appeal.
   - Dispute requires revised amount + reason.
   - **Submit taxpayer response** → Under Review (1104).
7. Officer outcome (depends on type):
   - Accept → **BOD Confirmed** or **Cancelled** → Closed Case (1107)
   - Dispute → **Amend** → Fieldwork (1101), or **No Change** → new TXR row and back to 1103
   - Appeal → **Escalated to Appeal** → Closed For Appeal (1106)

## Statuses

| Id | Label |
|----|--------|
| 1100 | Pending Audit Case Creation (draft) |
| 1101 | Fieldwork |
| 1102 | Pending Audit Approval |
| 1103 | Audit Approved - Pending Taxpayer |
| 1104 | Under Review |
| 1106 | Closed For Appeal |
| 1107 | Closed Case |

## APIs

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/v1/audit-cases?search=` | Listing |
| POST | `/api/v1/audit-cases` | Draft |
| GET | `/api/v1/audit-cases/{id}` | Detail (seeds taxpayer-response row if status is 1103/1104) |
| PUT | `/api/v1/audit-cases/{id}` | Save draft |
| POST | `/api/v1/audit-cases/{id}?command=submit` | Create + ingest + seed planning |
| PUT | `/api/v1/audit-cases/{id}/planning` | Planning + proposed case type |
| PUT | `/api/v1/audit-cases/{id}/field-work` | Fieldwork (Field case type) |
| POST | `/api/v1/audit-cases/{id}/working-papers` | Add draft working paper |
| PUT | `/api/v1/audit-cases/{id}/working-papers/{wpId}` | Save working paper |
| PUT | `/api/v1/audit-cases/{id}/findings` | Officer save/submit, or `{ "supervisor": true, "supervisorStatus": 1\|2 }` |
| PUT | `/api/v1/audit-cases/{id}/taxpayer-response` | Staff taxpayer reply (`submit: true`) or `{ "officer": true, "officersFinalOutcome": 1–5 }` |

Restart backend after pull (`.\run-dev.ps1`). Frontend: `cd assist-web-app; npm start` → `/audit`.
