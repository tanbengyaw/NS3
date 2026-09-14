# Incomplete auto-registration (audit ingest)

**Status:** Phase 1 — sales-first, all tax types supported on ingest/listing/complete.

Incomplete auto-reg is **not** started from a blank wizard like Update / Discontinue tax. Audit (later `assist-audit`) plants a live employer + partial `SstInfo`. Registration only **lists** incomplete rows and lets staff **complete** them (sections 1205–1209).

## Flow

```
Audit Create Case (or Simulate ingest on the listing page)
    POST /api/v1/sst-auto-registrations
         ↓
Employer + SstInfo
  is_auto_registration = true
  auto_registration_source = audit
  no 1205–1209 case yet
         ↓
Staff: Registration → Incomplete auto-reg
         ↓
POST /api/v1/registration-cases/incomplete-auto-regs  { sstInfoId }
         ↓
Temp draft → existing SST wizard (sales / service / …)
         ↓
Submit → APPROVED (non-UO; existing RegistrationCaseSubmitRouter)
         ↓
Same SstInfo row gets SMK; flag stays true
```

## APIs

| Method | Path | Who |
|--------|------|-----|
| POST | `/sst-auto-registrations` | Audit module / staff simulate |
| GET | `/reference/incomplete-auto-regs?taxType=&search=` | Staff listing |
| POST | `/registration-cases/incomplete-auto-regs` | Staff start/resume completion |

Completeness gate (listing):

- Sales / service: `manComDate` **and** `dateSaleValTaxGoods`
- Tourism / digital / DPSP: `applicantName`

## UI

Staff-only: `/registration/incomplete-auto-reg`

Staff Audit Create Case: `/audit` → New case → Create case. That path calls `POST /sst-auto-registrations` and writes `employer_id` / `sst_info_id` back onto the audit rows. The listing page still has a simulate-ingest fallback.

Audit tables: Liquibase `0045_audit_schema.xml` + `0046_audit_cus_aud_ref.xml` → schema `audit`.

## Related

- Submit routing: [submit-routing.md](./submit-routing.md)
- Admin menu: [admin-menu-scope.md](./admin-menu-scope.md)
- Audit Create Case through taxpayer response: [audit-create-case.md](./audit-create-case.md)
