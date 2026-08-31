# SST stakeholder workshop — facilitator guide

Run this session **before** coding SST flows in `NS3_new_framewrok`. The goal is signed-off **Phase 1 scope** and enough field/workflow detail to implement **one tax type** as a vertical slice.

**Companion doc (fill during workshop):** [sst-discovery-questions.md](./sst-discovery-questions.md)

---

## 1. Workshop outcomes (must leave with these)

| # | Decision | Owner | Done? |
|---|----------|-------|-------|
| D1 | Phase 1 tax types (pick 1–2 to build first) | Business | ☐ |
| D2 | Phase 1 flows: new reg / update / incomplete / discontinue | Business | ☐ |
| D3 | Portal vs OTC-only per tax type | Business | ☐ |
| D4 | JKDM approval model (manual vs integration) | JKDM liaison | ☐ |
| D5 | Mandatory fields for chosen tax type (Part A/B/C) | Registration SME | ☐ |
| D6 | Numbering on approve (SST reg no vs SMK ref) | Registration SME | ☐ |
| D7 | Replatform target date / priority vs SOCSO reg | Project owner | ☐ |

If D1–D4 are not answered, **do not start SST development**.

---

## 2. Who to invite

| Role | Why they must attend | Can delegate? |
|------|----------------------|---------------|
| **Registration business owner (SST)** | Scope and priority | No |
| **PERKESO registration SME** | Legacy ASSIST behaviour, forms, workflow | No |
| **JKDM / Customs liaison** | Approval rules, SST02 outstanding checks, legal definitions (digital vs DPSP) | Partial — need written follow-up |
| **Replatform tech lead** | Feasibility, API/data model | Yes |
| **Officer (counter user)** | OTC/incomplete auto-reg reality | Yes |
| **Portal support** | Employer self-service rules | Optional for session 1 |

**Minimum viable room:** business owner + registration SME + tech lead.  
**Schedule JKDM** for session 2 if they cannot join session 1.

---

## 3. Pre-workshop prep (facilitator)

Send invite **3–5 days** ahead with:

1. Link to this guide and [sst-discovery-questions.md](./sst-discovery-questions.md) §4 only (scope)
2. One-page context: replatform is **ASSIST registration slice**, not full JKDM system
3. Ask attendees to skim legacy screens if available:
   - Sales: `/sales-tax`
   - Tourism: `/tourism-tax`
   - Digital: `/digital-tax`
   - DPSP: `/dpsp-tax`
   - Service tax = standard `/new-reg` (section 200, prefix `-CP-`)

**Facilitator prints / shares on screen:**

- Section map from checklist §1 (SMK prefixes `-CP-` / `-CJ-` / `-CO-` / `-CD-` / `-CT-`)
- Current NS3 status: standard SOCSO reg done; SST **not started** (enums + code generators only)

**Duration:** 90 minutes (scope) + optional 60 minutes (per-tax deep dive)

---

## 4. Agenda — Session 1: Scope (90 min)

| Time | Topic | Checklist | Facilitator notes |
|------|-------|-----------|-------------------|
| 0:00–0:10 | Intro & goals | — | Show §1 section map. Stress: service tax ≠ section 1100. |
| 0:10–0:25 | **Phase 1 tax types** | §4.1, §4.6 | Force rank: service, sales, tourism, digital, DPSP. Pick **one pilot**. |
| 0:25–0:35 | **Phase 1 flows** | §4.1 | New reg only? Include update 1200–1204? Incomplete 1205–1209? Discontinue 1103? |
| 0:35–0:45 | **Channel** | §4.3 | Portal vs counter per tax. |
| 0:45–0:55 | **JKDM / SST02** | §5.11, §4.4 | Updates “pending JKDM approval” — still true? Block on outstanding tax? |
| 0:55–1:05 | **One employer, many taxes** | §4.5, §9.1 | Single `SstInfo` row vs separate registrations. |
| 1:05–1:20 | **Decision recap & sign-off** | §12 | Read back D1–D7 aloud. |
| 1:20–1:30 | Buffer / parking lot | — | Capture “session 2” items. |

### Facilitator script — opening (2 min)

> We are replatforming ASSIST **registration** for SST taxpayers, not rebuilding JKDM’s full SST system. Legacy ASSIST has five tax paths under sections 1100–1109 and 1200–1209. Today we decide **what goes in Phase 1** so development can start on one vertical slice.

### Facilitator script — priority question

> If we can only deliver **one** SST registration type in the next sprint, which one is highest value: **sales (1100)**, **service (200)**, **tourism (1101)**, **digital (1102)**, or **DPSP (1104)**?

Record answer in D1.

### Parking lot (do not solve in session 1)

- Full tariff code / service type catalogues
- Letter/certificate templates
- jBPM parity
- Production PostgreSQL / area code master

---

## 5. Agenda — Session 2: Pilot tax deep dive (60 min)

Run only after D1 names the **pilot tax type**. Use checklist §6 for that type only.

| Time | Topic |
|------|-------|
| 0:00–0:15 | Walk legacy Form 1 JSP (screen share or PDF) |
| 0:15–0:30 | Mandatory vs optional fields (Part A, B, C) |
| 0:30–0:45 | Premises / directors / contact persons / special popups |
| 0:45–0:55 | Submit → approve workflow & special cases |
| 0:55–1:00 | Sign-off D5, D6 |

**Legacy JSP quick reference (pilot tax):**

| Pilot tax | Form 1 entry point |
|-----------|-------------------|
| Service | `new_reg/new_reg_form.jsp` |
| Sales | `new_reg_sst_sales_tax/new_reg_sst_sales_tax_form1.jsp` |
| Tourism | `new_reg_sst_tourism_tax/new_reg_sst_tourism_tax_form1.jsp` |
| Digital | `new_reg_sst_digital_tax/new_reg_sst_digital_tax_form1.jsp` |
| DPSP | `new_reg_sst_dpsp_tax/new_reg_sst_dpsp_tax_form1.jsp` |

---

## 6. Session 3 (optional): Update & incomplete (60 min)

Only if D2 includes update (1200–1204) or incomplete (1205–1209).

| Time | Topic | Checklist |
|------|-------|-----------|
| 0:00–0:20 | Update taxpayer — editable fields, new case vs amend | §7 |
| 0:20–0:40 | Incomplete auto-reg — data source, who creates cases | §8 |
| 0:40–0:55 | Discontinue tax (1103) | §5.12 |
| 0:55–1:00 | Sign-off | §12 |

---

## 7. Decision log (copy into meeting notes)

```text
Date:
Attendees:

D1 — Phase 1 tax types (pilot first):
D2 — Phase 1 flows (new / update / incomplete / discontinue):
D3 — Portal vs OTC:
D4 — JKDM approval / SST02 rules:
D5 — Mandatory fields (pilot tax):
D6 — IDs issued on approve:
D7 — Priority / target date:

Parking lot:
Action items:
  - [ ] Owner — Task — Due
```

---

## 8. Email invite template

**Subject:** SST registration replatform — scope workshop (90 min)

**Body:**

> Hi all,
>
> We are planning the ASSIST **SST registration** replatform (sales, service, tourism, digital, DPSP tax). Before development starts, we need your input on **Phase 1 scope**.
>
> **When:** [date/time]  
> **Where:** [room / Teams link]  
> **Pre-read:** [docs/sst-discovery-questions.md](./sst-discovery-questions.md) — section 4 only (15 min)
>
> **We will decide:**
> - Which tax type(s) are in Phase 1 (and which one is the pilot)
> - Which flows: new registration, update taxpayer, incomplete auto-reg, discontinue
> - Portal vs counter rules
> - JKDM approval expectations
>
> Please confirm attendance. JKDM representation is required for update/outstanding-tax questions (session 2 if not available).
>
> Thanks,  
> [Name]

---

## 9. After the workshop — dev handoff

When D1–D6 are filled, tech lead creates a **pilot slice ticket** with:

1. `RegistrationSection` enum value(s) in scope
2. Field list from §6 (pilot tax) with mandatory flags
3. Approve outputs: which columns on `SstInfo` / employer
4. Workflow: same as SOCSO reg or exceptions
5. Out of scope list (explicit)

**Suggested first implementation order** (after workshop):

```
Liquibase sst_info (minimal)
  → Form 1 API for pilot section
  → Submit router for pilot section
  → Approve → SstRegistrationNo + SMK prefix
  → One E2E test
  → Angular wizard variant (if UI in scope)
```

---

## 10. Related documents

| Document | Purpose |
|----------|---------|
| [sst-discovery-questions.md](./sst-discovery-questions.md) | Full question bank + legacy map |
| [../README.md](../README.md) | Replatform status |
| `assist-registration/.../RegistrationSection.java` | Section IDs in code |

---

## 11. Sign-off

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Business owner (SST) | | | |
| Registration SME | | | |
| JKDM liaison | | | |
| Tech lead | | | |
