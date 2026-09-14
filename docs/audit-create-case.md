# Audit Create Case

Staff-only slice that plants incomplete auto-reg. Not the full Audit product (planning, fieldwork, working papers, findings, BOD, taxpayer portal, Camunda are stubbed / not built).

## Officer path

1. Login as `officer_pj` / `password` (or `admin`).
2. **Audit → Audit cases → New case**.
3. Fill taxpayer (name, **new** BRN, postcode, branch) and tax-type popup fields (business commencement, FYE month; sales/service also man/ser commencement + annual value).
4. **Create case**.

If `employerId` is still null, backend calls `POST /sst-auto-registrations` (does **not** send `dateSaleValTaxGoods`), then writes `employer_id` / `sst_info_id` onto the audit rows. Status becomes Fieldwork (1101).

5. **Registration → Incomplete auto-reg → Search** — the planted row should appear. **Complete** as today.

## APIs

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/v1/audit-cases?search=` | Listing |
| POST | `/api/v1/audit-cases` | Draft |
| GET | `/api/v1/audit-cases/{id}` | Detail |
| PUT | `/api/v1/audit-cases/{id}` | Save draft |
| POST | `/api/v1/audit-cases/{id}?command=submit` | Create + ingest |

Restart backend after pull so Liquibase `0045` / `0046` apply (`.\run-dev.ps1`). Frontend: `cd assist-web-app; npm start` → `/audit`.
