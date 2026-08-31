# SST discovery checklist — Sales / Service / Tourism / Digital / DPSP tax

Stakeholder questionnaire for replatforming ASSIST SST registration flows into `NS3_new_framewrok`.

**Workshop guide:** [sst-discovery-workshop.md](./sst-discovery-workshop.md) — agenda, invite list, decision log, email template.

**How to use:** Work top-down. Tick answers in the **Answer** column during workshops. Cross-check legacy ASSIST paths in `assist_tesing_only` (or your local ASSIST checkout) before signing off replatform scope.

---

## 0. Decisions summary (recorded)

| ID | Decision | Answer |
|----|----------|--------|
| **D1** | Phase 1 tax types (pilot first) | **Sales + service** (§4.6); pilot build = **sales tax (1100)** |
| **D2** | Phase 1 flow order | **New reg → update → incomplete → discontinue** (§4.1) |
| **D3** | Channel | Portal self-service **and** officer OTC (§4.3) |
| **D4** | Approval roles | **RO** = new reg (200, 1100–1102, 1104); **UO** = update (1200–1204) + discontinue (1103); manual in ASSIST, no external JKDM API (§4.4) |
| **D5** | Multi-tax data model | One **employer** (one BRN); **many `SST_INFO` rows** — sales = many rows (each own `salesTaxSmkRegNo`); service/tourism/digital/DPSP = max 1 active row each (§4.5) |
| **D6** | IDs on approve | SST registration number + tax-specific SMK; both on letters (§5.1) |
| **D7** | Service tax entry | Standard **`REG_NEW_REG` (200)** wizard — Form 1/2 + employees + service-type codes (§4.2, §6.1) |
| **D8** | Sales Form 1/2 | Directors **mandatory**; premises **optional**; tariff on `SstInfo`; Part B five fields + breakdown mandatory (§5.6–5.7, §6.2) |
| **D9** | Update / incomplete | Update = **new case**, merge in place on approve, **UO** queue; incomplete = **audit WS**, staff-only (§7, §8) |
| **D10** | Discontinue / SST02 | One tax type per discontinue case; SST02 outstanding **blocks discontinue form only** (§5.11–5.12) |
| **D11** | Data model NS3 | `sst_info` + children in **`assist-registration`**; status per tax type on each row (§9) |

### Completion status

| Section | Status |
|---------|--------|
| §4 Scope & priority | **Complete** (incl. JKDM = UO-on-behalf, §4.4) |
| §5 Cross-cutting (5.1–5.13) | **Complete** |
| §6.1 Service tax | **Complete** |
| §6.2 Sales tax | **Complete** |
| §6.3 Tourism tax | **Complete** (documented; **deferred** from Phase 1 pilot) |
| §6.4 Digital tax | **Complete** (documented; **deferred** from Phase 1 pilot) |
| §6.5 DPSP tax | **Complete** (documented; **deferred** from Phase 1 pilot) |
| §7 Update taxpayer | **Complete** |
| §8 Incomplete auto-reg | **Complete** (§8.4 SLA — ops confirm outside codebase) |
| §9 Data model | **Complete** (§9.5 migration — recommendation recorded) |
| §12 Sign-off | **Template only** — names still *TBD* at workshop |

**Discovery checklist: complete.** Ready to code Phase 1 pilot (**sales new reg 1100**).

### Genuinely still open (non-blocking)

| Item | Status |
| ---- | ------ |
| §12 sign-off **names/dates** | Pending real workshop attendees (*TBD* in table) |
| §8.4 incomplete **SLA / reminders** | Not in legacy code — confirm with operations if needed |
| §9.5 migration **1:1 cutover** | Recommendation only — business sign-off at §12 |
| External **JKDM API** | Not required for legacy parity; future enhancement only |
| §6.3–6.5 implementation | Documented; **build after** sales + service pilot (§4.6) |

---

**Related replatform code:**

| Item                          | Location                                                                               |
| ----------------------------- | -------------------------------------------------------------------------------------- |
| Section enum (mirrors ASSIST) | `assist-registration/.../RegistrationSection.java`                                     |
| SMK prefix per tax            | `assist-registration/.../SmkNoGenerator.java` (`-CP-`, `-CJ-`, `-CO-`, `-CD-`, `-CT-`) |
| SST registration no           | `assist-registration/.../SstRegistrationNoGenerator.java`                              |
| Tax type enum (legacy)        | `assist_tesing_only/.../TaxTypeEnum.java`                                              |
| Shared SST entity (legacy)    | `assist_tesing_only/.../pojo/SstInfo.java`                                             |


---



## 1. Section and numbering map


| Tax type        | `TaxTypeEnum`     | New registration section           | SMK prefix | Update section                            | Incomplete auto-reg section                   |
| --------------- | ----------------- | ---------------------------------- | ---------- | ----------------------------------------- | --------------------------------------------- |
| **Service tax** | `SERVICE_TAX` (1) | `REG_NEW_REG` **(200)** — not 1100 | `-CP-`     | `REG_UPDATE_TAX_PAYER_SERVICE_TAX` (1200) | `REG_INCOMPLETE_TAX_PAYER_SERVICE_TAX` (1205) |
| **Sales tax**   | `SALES_TAX` (2)   | `REG_NEW_REG_SST_SALES_TAX` (1100) | `-CJ-`     | `REG_UPDATE_TAX_PAYER_SALES_TAX` (1201)   | `REG_INCOMPLETE_TAX_PAYER_SALES_TAX` (1206)   |
| **Tourism tax** | `TOURISM_TAX` (3) | `REG_SST_TOURISM_TAX` (1101)       | `-CO-`     | `REG_UPDATE_TAX_PAYER_TOURISM_TAX` (1202) | `REG_INCOMPLETE_TAX_PAYER_TOURISM_TAX` (1207) |
| **Digital tax** | `DIGITAL_TAX` (4) | `REG_SST_DIGITAL_TAX` (1102)       | `-CD-`     | `REG_UPDATE_TAX_PAYER_DIGITAL_TAX` (1203) | `REG_INCOMPLETE_TAX_PAYER_DIGITAL_TAX` (1208) |
| **DPSP tax**    | `DPSP_TAX` (5)    | `REG_SST_DPSP_TAX` (1104)          | `-CT-`     | `REG_UPDATE_TAX_PAYER_DPSP_TAX` (1204)    | `REG_INCOMPLETE_TAX_PAYER_DPSP_TAX` (1209)    |


**Other SST sections**


| Section                   | ID   | Notes                        |
| ------------------------- | ---- | ---------------------------- |
| `REG_SST_DISCONTINUE_TAX` | 1103 | Discontinue tax registration |


**Number formats (confirm with business)**


| ID type                  | Legacy generator                                   | Example pattern                                                                                         |
| ------------------------ | -------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| SST registration no      | `SstRegistrationNo` / `SstRegistrationNoGenerator` | `{areaCode}-{M}{YYYY}-{8-digit}`                                                                        |
| SMK case ref             | `SmkNo` / `SmkNoGenerator`                         | `{areaCode}-{prefix}-{8-digit}/{year}`                                                                  |
| Per-tax SMK on `SstInfo` | columns on approve                                 | `serviceTaxSmkRegNo`, `salesTaxSmkRegNo`, `tourismTaxSmkRegNo`, `digitalTaxSmkRegNo`, `dpspTaxSmkRegNo` |


---



## 2. Legacy ASSIST reference map

Paths relative to `assist/module/registration/registration-root/trunk/`.

### Shared UI


| Purpose                                | JSP / enum                                                               |
| -------------------------------------- | ------------------------------------------------------------------------ |
| Part A — business particulars (shared) | `registration-portlet/.../common/common_sst_tax_part_a_form.jsp`         |
| Part C — declaration                   | `registration-portlet/.../common/common_sst_part_c.jsp`                  |
| Search existing tax info               | `registration-portlet/.../common/common_search_tax_info_form.jsp`        |
| Search taxpayer (update entry)         | `registration-portlet/.../search_for_tax_payer/search_for_tax_payer.jsp` |




### New registration — by tax type


| Tax     | Layout URL (`RegLayoutUrl`) | Form 1                                                                         | Form 2                      | Preview                       | Counter (submit logic)                | Portlet                      |
| ------- | --------------------------- | ------------------------------------------------------------------------------ | --------------------------- | ----------------------------- | ------------------------------------- | ---------------------------- |
| Service | `/new-reg` (standard)       | `new_reg/new_reg_form.jsp`                                                     | `new_reg/new_reg_form2.jsp` | `new_reg/new_reg_preview.jsp` | `NewSstRegCounter` / standard new reg | standard new reg portlet     |
| Sales   | `/sales-tax`                | `new_reg_sst_sales_tax/new_reg_sst_sales_tax_form1.jsp` (+ `form1_part_a.jsp`) | `.../form2.jsp`             | `.../preview.jsp`             | `NewRegSstSalesTaxCounter`            | `NewRegSstSalesTaxPortlet`   |
| Tourism | `/tourism-tax`              | `new_reg_sst_tourism_tax/form1.jsp`                                            | `.../form2.jsp`             | `.../preview.jsp`             | `NewRegSstTourismTaxCounter`          | `NewRegSstTourismTaxPortlet` |
| Digital | `/digital-tax`              | `new_reg_sst_digital_tax/form1.jsp`                                            | `.../form2.jsp`             | `.../preview.jsp`             | `NewRegSstDigitalTaxCounter`          | `NewRegSstDigitalTaxPortlet` |
| DPSP    | `/dpsp-tax`                 | `new_reg_sst_dpsp_tax/form1.jsp`, `form_part_a.jsp`, `form_part_b.jsp`         | `.../form2.jsp`             | `.../preview.jsp`             | `NewRegSstDpspTaxCounter`             | `NewRegSstDpspTaxPortlet`    |




### Update taxpayer


| Tax                | Form 1                                                                       | Form 2                      | Counter                     |
| ------------------ | ---------------------------------------------------------------------------- | --------------------------- | --------------------------- |
| All (search first) | `update_tax_payer_info/update_tax_payer_info_form1.jsp` + tax-specific form1 | tax-specific form2          | `UpdateTaxPayerInfoCounter` |
| Service            | `update_tax_payer_info_service_tax_form1.jsp`                                | `..._service_tax_form2.jsp` |                             |
| Sales              | `update_tax_payer_info_sales_tax_form1.jsp`                                  | `..._sales_tax_form2.jsp`   |                             |
| Tourism            | `update_tax_payer_info_tourism_tax_form1.jsp`                                | `..._tourism_tax_form2.jsp` |                             |
| Digital            | `update_tax_payer_info_digital_tax_form1.jsp`                                | `..._digital_tax_form2.jsp` |                             |
| DPSP               | `update_tax_payer_info_dpsp_tax_form1.jsp`                                   | `..._dpsp_tax_form2.jsp`    |                             |


Legacy notification on update submit: *"Update information is pending JKDM approval"* (`IntegrationMethodWrapper.sendNoticeToTaxPayer`).

### Incomplete auto-registration


| Tax     | Form 1                                                            | Form 2          | Preview                       | Counter                       |
| ------- | ----------------------------------------------------------------- | --------------- | ----------------------------- | ----------------------------- |
| Entry   | `incomplete_auto_reg_tax_payer/incomplete_auto_reg_tax_payer.jsp` |                 | shared preview                | `IncompleteAutoRegTaxCounter` |
| Service | `incomplete_auto_reg_service_tax_form1.jsp`                       | `..._form2.jsp` | `..._service_tax_preview.jsp` |                               |
| Sales   | `incomplete_auto_reg_sales_tax_form1.jsp`                         | `..._form2.jsp` | (shared / sales preview)      |                               |
| Tourism | `incomplete_auto_reg_tourism_tax_form1.jsp`                       | `..._form2.jsp` | `..._tourism_tax_preview.jsp` |                               |
| Digital | `incomplete_auto_reg_digital_tax_form1.jsp`                       | `..._form2.jsp` | `..._digital_tax_preview.jsp` |                               |
| DPSP    | `incomplete_auto_reg_dpsp_tax_form1.jsp`                          | `..._form2.jsp` | `..._dpsp_tax_preview.jsp`    |                               |




### Discontinue tax


| UI                                                                              | Counter                 |
| ------------------------------------------------------------------------------- | ----------------------- |
| `discontinue_tax_business/discontinue_tax_business_form.jsp`, `..._preview.jsp` | `DiscontinueTaxCounter` |




### Key DTOs / services (legacy)


| Area                        | Class                                                                |
| --------------------------- | -------------------------------------------------------------------- |
| Shared Part B/C SST fields  | `SstInfoDto`                                                         |
| Sales Form 1                | `NewRegSstSalesTaxForm1Dto`                                          |
| DPSP Form 2                 | `SstDpspTaxForm2Dto`                                                 |
| Service tax Form 1 (update) | `SstServiceTaxForm1Dto`                                              |
| View/search update          | `UpdateTaxPayerInfoViewServiceImpl`                                  |
| Outstanding tax check       | `IntegrationMethodWrapper.verifyOutstandingAmtForTax` → SST02 module |
| SST status                  | `SstStatusTypeEnum` (Active / Cancel)                                |




### `SstInfo` child collections (data model hints)


| Collection                                             | Typical tax types        |
| ------------------------------------------------------ | ------------------------ |
| `sstServiceCategorySet`                                | Service tax              |
| `sstTariffCodeSalesCategorySet`                        | Sales tax                |
| `accommodationPremisesSet`                             | Tourism tax              |
| `digitalServiceSet`                                    | Digital tax              |
| `contactPersonSet`                                     | Service, tourism, others |
| `motacRegNo`, `accomodationType`, `rating`, `noOfRoom` | Tourism tax              |
| `websiteAddress`                                       | Digital / DPSP           |


---



## 3. NS3 replatform status (Phase 1)


| SST capability                                               | Status                                         |
| ------------------------------------------------------------ | ---------------------------------------------- |
| Standard employer new reg (`REG_NEW_REG` / service tax path) | **Done** (cases, submit, approve, employees)   |
| SST-specific sections (1100–1109, 1200–1209)                 | **Not started** — enums + code generators only |
| `SstInfo` entity / Liquibase                                 | **Not started**                                |
| Update taxpayer / incomplete auto-reg / discontinue          | **Not started**                                |
| JKDM / SST02 integration                                     | **Not started**                                |


---



## 4. Scope and priority (ask first)


| #   | Question                                                                                      | Answer                                                                                            |
| --- | --------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| 4.1 | Which flows are in **Phase 1** vs later? (new reg / update / incomplete / discontinue)        | **Phase 1 (in build order):** new reg → update → incomplete → discontinue. All four in scope; implement sequentially (sales + service first per §4.6). |
| 4.2 | Confirm **service tax** still uses standard `REG_NEW_REG` (200), not a dedicated 110x section | yes                                                                                               |
| 4.3 | Which tax types are **portal self-service** vs **officer OTC only**?                          | Both Portal self-service and Officer OTC are available for SST tax registration.                  |
| 4.4 | Is **JKDM approval** still required for updates? External API or manual in ASSIST?            | **Yes for Update Tax Payer (1200–1204) and Discontinue Tax (1103) — manual approval inside ASSIST.** BPM routes in-progress cases to **`RoleEnum.UO`** (`REGISTRATION_ROUTE_TO_UO` — `UpdateTaxPayerWorkflowServiceImpl`, `DiscontinueTaxWorkflowServiceImpl`). Preview approve/reject: **UO + EO** (`RegRolePermission.updateEmployerInfoPreview`). Employer messaging: *"Update information is pending JKDM approval"* (`IntegrationMethodWrapper.sendNoticeToTaxPayer`); success title `update_case_route_jkdm` (`NewRegSuccessDto`). **JKDM = business label only in legacy — not an external JKDM approval API.** `IntegrationMethodWrapper` wraps PERKESO internals (`WorkflowWs`, `SSTNotificationWs`, SST02 payment lookup); no JKDM web service for approve/reject. **Interpretation for replatform:** PERKESO **UO officer acts on JKDM's behalf** inside ASSIST workflow; employer-facing copy says "JKDM approval" but approval is in-system UO task queue. **UO is not the new-reg approver:** SST new reg (1100–1102, 1104) and service tax (`REG_NEW_REG` 200) route to **RO** (`REGISTRATION_ROUTE_TO_RO`; **PKR_BO** if incomplete from OTC/employer). **Incomplete auto-reg (1205–1209):** no BPM route to UO; non-UO submitters auto-approve in `IncompleteAutoRegTaxCounter`. **NS3:** model **RO = new reg approval**, **UO = update/discontinue approval** (retain JKDM label on employer notifications if business requires). |
| 4.5 | Can one employer hold **multiple active SST registrations** (e.g. service + sales)?           | **Yes — one `Employer` (one BRN), multiple `SST_INFO` rows.** Per tax type: **service, tourism, digital, DPSP = max 1 active row each** (one SMK column populated per row: `serviceTaxSmkRegNo`, `tourismTaxSmkRegNo`, etc.). **Sales tax = many rows** — each sales registration is a **separate `SST_INFO` row** with its **own `salesTaxSmkRegNo`** (`-CJ-` SMK); **not** multiple sales SMK values on one row. Same employer/BRN can hold e.g. service + sales + tourism concurrently. **Multiple premises** (employer `PREMISES` table) are separate from “multiple sales tax”: premises/manufacturing sites attach to **employer**; each sales **registration** gets tariff codes on **its** sales `SstInfo` (`sstTariffCodeSalesCategorySet`). Audit auto-reg WS allows two `SALES_TAX` entries in one call → two `SstInfo` rows (`TestAutoRegTaxPayerWs.testDuplicateSalesTaxType_isAllowed`). Ref: `SstInfo` entity, `AbstractRegCounter.submitSstInfoRecord`, `EmployerImpl.isAuditAutoRegSstDataCompleteForTaxType`, incomplete-auto-reg cardinality rule. |
| 4.6 | Priority order if not all five tax types fit Phase 1?                                         | **sales and service**                                                                             |


---



## 5. Cross-cutting SST questions


| #    | Topic           | Question                                                                                                                                                 | Answer                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| ---- | --------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 5.1  | IDs             | On approve, which numbers are issued: SST reg no, SMK case ref, per-tax SMK column — and which appear on letters?                                        | On approval, system issues SST registration number and tax-specific SMK number. Letters should display SST registration number and the relevant tax-specific SMK number.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| 5.2  | BRN             | Same duplicate-BRN rules as SOCSO new reg? Any Customs/JKDM pre-reg lookup by BRN?                                                                       | What exists today:- BRN search (SST New Reg / common SST Form 1) — internal ASSIST only: - `EmployerImpl.searchEmployerByBrn(registrationNo)` → auto-populate employer/SST fields if found. - No external Customs/JKDM call.- Customs / pre-reg fields — manual officer entry, not BRN-triggered lookup: - `customAudRefNo` (Customs audit ref) - `preRegNo`, `preRegName`, `replacementDate` (business-succession / pre-registration block) - Present on New Reg SST, Update Tax Payer, and Incomplete Auto-Reg Form 1 JSPs; saved to `TempEmployer` / `SstInfo`.- JKDM references in code — workflow messaging only (e.g. update case routed to JKDM, EFT pending JKDM approval), not a BRN pre-reg lookup API.`AutoRegTaxPayerComponent` also has no customs/pre-reg fields — audit WS accepts BRN + address + tax blocks only.---Short answers you can give back- Duplicate BRN: Same principle as SOCSO New Reg (block new employer creation for an existing BRN), but auto-reg WS is a hard reject with no AEC/detection exception and no UI warning layer.- Customs/JKDM pre-reg by BRN: No — only manual fields today; BRN search hits ASSIST DB only. If audit wants JKDM pre-reg auto-fill, that would be new integration work, not existing behaviour.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| 5.3  | Part A          | Are Part A fields identical for sales / tourism / digital / DPSP, or tax-specific (e.g. `tourismTaxRegNo`, `incomeTaxRefNo`, `customAudRefNo` on sales)? | **Not identical** — Part A shares a common employer/address core, but each tax type adds or omits tax-specific fields.**Shared core (all four)**Most forms include some combination of:- Business type / registration identifier- Registered business name + address (state, city, postcode)- SOCSO office location- Trade name (+ “same as business name” checkbox)- Correspondence address block- Tel / fax / email (exact layout varies)---**Tax-specific Part A fields***Service tax Part A matches sales (same `common_sst_tax_part_a_form.jsp` pattern). **Incomplete auto-reg digital has a hidden `preRegInfoSection` but no visible checkbox block like sales. ***Incomplete auto-reg DPSP has pre-reg checkbox/fields; New Reg DPSP Part A does not.---**By tax type (summary)****Sales / Service** — richest Part A (`common_sst_tax_part_a_form.jsp` / `new_reg_sst_sales_tax_form1_part_a.jsp`):- BRN + search- `tourismTaxRegNo`, `incomeTaxRefNo`, `customAudRefNo`- Pre-reg succession block- Sales also embeds **premises table** in Part A**Tourism** — different identity model:- `businessType`, `identificationCardNo` (not standard BRN field), `motacRegistrationNo`- New Reg also has `labuan`, `sstRegistrationNo`, `newRegisterTax`- **No** customs / income-tax / pre-reg fields**Digital** — BRN + contact-heavy:- `registrationNo`, business/address core, `websiteAddress`, `businessEmailAddress`- New Reg also has `financialYearEndMonth` + authorised-person table in Part A- **No** `tourismTaxRegNo` / `incomeTaxRefNo` / `customAudRefNo` / pre-reg block**DPSP** — similar to digital for contact, simpler header:- `businessRegistrationNo`, business/address core, `websiteAddress`, `businessEmailAddress`- **No** customs / tourism / pre-reg fields in New Reg Part A---**Incomplete Auto-Reg mirrors the same split**- **Sales & service** → same extended block as sales New Reg (customs + pre-reg fields).- **Tourism** → `identificationCardNo` + `motacRegistrationNo`.- **Digital & DPSP** → `websiteAddress`; no customs trio.So your example is correct: `tourismTaxRegNo`**,** `incomeTaxRefNo`**,** `customAudRefNo` **are sales/service Part A fields only**, not shared across tourism, digital, or DPSP. |
| 5.4  | Part B          | Mandatory fields per tax: `businessComDate`, `manComDate`, `dateSaleValTaxGoods`, `anTotalTaxSalesVal`, `finYrEndMon`, subcontract flag?                 | **Not uniform — depends on layer.** UI mapping: `dateSaleValueTaxable`→`dateSaleValTaxGoods`; `annualTaxSale`→`anTotalTaxSalesVal`; `financialYearEndMonth`→`finYrEndMon`; `subContractWork`=optional flag (if set, sub-contract child list required). **Audit auto-reg WS** (`AutoRegTaxPayerLibServiceImpl`): all taxes `businessComDate`+`finYrEndMon`; sales+service also `manComDate`+`anTotalTaxSalesVal`; WS does not validate/persist `dateSaleValTaxGoods`; no subcontract. **Incomplete listing complete** (`isAuditAutoRegSstDataCompleteForTaxType`): sales/service=`manComDate` AND `dateSaleValTaxGoods`; tourism/digital/dpsp=`applicantPersonal` only. **Form 2 validation** (new reg + incomplete auto-reg): **Sales** — all five Part B fields + sales breakdown; subcontract conditional. **Service** — all five + `finYrEndMon`; subcontract conditional. **Tourism/DPSP** — `finYrEndMon` + begin-operation date; not sales/service date trio. **Digital Form 2** — DS types, `achievingValueOfDsDate`, `dsTotalValue`, `beginOperationDate`; `finYrEndMon` on Form 1. Ref: `SpecificTaxComponent`, `IncompleteAutoRegTaxPayerPortlet`, `NewRegSstSalesTaxPortlet`, `NewRegPortlet.validateForm2SaveNContinue`. |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| 5.5  | Part C          | Applicant declaration — same rules for all types?                                                                                                        | yes                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| 5.6  | Premises        | Required for which types? Sales (generic premises + tariff) vs tourism (accommodation premises)?                                                         | **Sales:** optional **additional** `Premises` on **`Employer`** (Form 1 Part A table + popup). Per-row fields mandatory on save (`NewRegSstSalesTaxPortlet.validateNewRegSstSalesPremisesForm`); **no “≥1 premise” submit check**. **Tourism:** **mandatory ≥1** `AccommodationPremises` on **`SstInfo`** (Form 2 Part B) — `reg.msg.accomodation_premises_list_is_empty` (`AbstractRegAction.validateSstTourismTaxForm2SaveCont`). Per-row: accommodation type, rating, room count. **Service / Digital / DPSP new reg:** no premises / accommodation-premises UI. Update + incomplete auto-reg mirror same split (sales premises table optional; tourism accommodation mandatory on Form 2). |
| 5.7  | Directors       | Required for sales only, or others?                                                                                                                      | **Mandatory ≥1 on Form 1** for **Sales, Service (`REG_NEW_REG`), Digital** (labelled “Authorised Personnel” but uses `DirectorOwner` → `Employer`) — `reg.msg.director_owner_list_is_empty` (`AbstractRegAction.validateCommonSstForm1`). **Tourism, DPSP:** no director section / no empty-list check. **Update / incomplete Form 1:** same — sales/service/digital require directors; tourism uses mandatory inline `contactPerson` instead. Hidden flag `hasDirectorOwnerList` from table row count. |
| 5.8  | Contact persons | Service/tourism contact-person popups — required for which types?                                                                                        | **Form 2 popup** (`Personal` → `SstInfo.contactPersonSet`): **sales, service, tourism** (new reg, update, incomplete); **DPSP incomplete Form 2 only**; **digital** uses authorised personnel on Form 1 instead. **No list-empty check on submit** — only per-row name + email on popup save. **Tourism Form 1 inline `contactPerson` field is mandatory** (separate from popup list). |
| 5.9  | Workflow        | Submit → approve / reject / query / incomplete — same as SOCSO reg or SST-specific routing?                                                              | **Same status model** as SOCSO (NEW → IN_PROGRESS / APPROVED / REJECTED / IN_QUERY / CLOSED) via `RegWorkflowDriver`. **SST-specific routing:** new reg SST + service tax → **RO** (PKR_BO if incomplete from OTC/portal employer); update 1200–1204 → **UO** (+ JKDM messaging); discontinue 1103 → **UO**; incomplete 1205–1209 → **no BPM route** in `RegWorkflowDriver` — non-UO submitters auto-approve in `IncompleteAutoRegTaxCounter`. SST lacks SOCSO under-enforcement / AD04 inspection paths; new reg preview reject → **CLOSED** (not REJECTED). |
| 5.10 | Letters         | Which acknowledgement letters / certificates per tax type?                                                                                               | **New reg + incomplete (per tax):** `AssignationLetterService` — service (`REG_NEW_REG`, 1205) → `NewRegLetterImpl.getLetterOfSSTRegAck`; sales (1100, 1206) → sales ack; tourism (1101, 1207) → ack + certificate; digital (1102, 1208) → digital ack; DPSP (1104, 1209) → DPSP ack. IN_QUERY / REJECTED use appendix inquiry/rejection on new-reg letter impls. **Update 1200–1204 and discontinue 1103: no letter service wired** (switch falls through to null). |
| 5.11 | SST02           | When must `verifyOutstandingAmtForTax` block or warn (before update, discontinue, approve)?                                                              | **Blocks discontinue tax form save only** — `SearchForTaxPayerPortlet.validateDiscontinueTaxFormSaveCont` → `IntegrationMethodWrapper.verifyOutstandingAmtForTax` (queries SST02 by `sstInfo.id` + tax type; true if total outstanding or penalty > 0) → action error `reg.msg.tax_has_outstanding_amt`. **Not called** on update submit, new reg approve, or discontinue counter approve. Tax type inferred from first non-blank SMK column on `SstInfo`, not always `dto.taxTypeId`. |
| 5.12 | Discontinue     | Who initiates; impact on other active tax types on same employer?                                                                                        | **Staff initiates** via Search for Tax Payer → discontinue tax flow (section 1103, `DiscontinueTaxCounter`). One case per **selected tax type** — loads one `SstInfo` by SMK + employer; on approve updates `SstStatusInfo` for that **`taxTypeId` only** (Cancel). **Other active tax types on same employer / other `SstInfo` rows are untouched.** SST02 outstanding check blocks form entry before case creation. |
| 5.13 | Auto-reg        | Meaning of `isAutoRegistration` / `autoRegistrationSourceFrom` on `SstInfo` — still used?                                                                | **Set by audit auto-reg WS:** `isAutoRegistration=true`, `autoRegistrationSourceFrom="audit"` (`AutoRegTaxPayerLibServiceImpl`). Manual new reg sets `false` (`AbstractRegCounter`). **Still used:** incomplete listing filter (`EmployerImpl.searchIncompleteAutoRegListing`); audit rows excluded from normal tax-payer search (`isAuditAutoRegSstInfo`). Flag **persists after staff completion** — not cleared on incomplete submit. |




---



## 6. Per tax type — discovery questions



### 6.1 Service tax (`REG_NEW_REG`, `-CP-`)


| #     | Question                                                                               | Legacy reference                    | Answer |
| ----- | -------------------------------------------------------------------------------------- | ----------------------------------- | ------ |
| 6.1.1 | Is service tax still the **standard new registration** wizard (Form 1/2 + employees)?  | `new_reg/new_reg_form.jsp`          | **Yes** — Form 1 → Form 2 → eligible employees (if SIP-eligible temp employees exist) → employee registration + service-type codes → supporting docs → preview → success. Counter: `NewSstRegCounter` on section `REG_NEW_REG` (200). |
| 6.1.2 | **Service type codes** — full catalogue required? JKDM validation?                     | `service_type_code_pop_up_form.jsp` | **Full `REF_SST_SERVICE_TYPE` catalogue** required (`SstServiceType` ref: code, rate, threshold, group). Popup saves to `TempSstServiceCategory` / `SstServiceCategory`. **No JKDM validation** on selection — portlet only validates group/type chosen. `SstServiceType.threshold` exists on ref entity but **not enforced** in registration Java. |
| 6.1.3 | **Eligible employees** — same SOCSO rules or SST-specific thresholds?                  | `new_reg_eligible_employee.jsp`     | **Same SOCSO EIS rules** — temp employees with `sipStartDt` and appStatus `NONE` or `IN_QUERY` (`TempEmployeeImpl.countEligibleSipEmployeeByRefNo`). If count > 0, wizard routes to eligible-employee step. No SST-specific threshold. |
| 6.1.4 | On approve: populate `serviceTaxSmkRegNo` only, or also SST registration number?       | `SstInfo.serviceTaxSmkRegNo`        | **Both.** One generated SMK written to **`smkRegNo` and `serviceTaxSmkRegNo`** on new `SstInfo` create (`AbstractRegCounter.submitSstInfoRecord`). SST registration number (`SstRegistrationNo.generateCode`) issued separately on employer/SST submit path. |
| 6.1.5 | **Incomplete auto-reg service tax** — who creates cases (JKDM batch, officer, system)? | `IncompleteAutoRegTaxCounter`       | **System (audit WS)** creates underlying employer + partial `SstInfo` (`AutoRegTaxPayerWs` → `AutoRegTaxPayerLibServiceImpl`, source `"audit"`). **Staff officer** opens incomplete listing and creates completion draft (section 1205) via `IncompleteAutoRegTaxCounter`. Not JKDM batch feed or employer portal self-service. |




### 6.2 Sales tax (`REG_NEW_REG_SST_SALES_TAX`, `-CJ-`)


| #     | Question                                                                                                                         | Legacy reference                                | Answer |
| ----- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------- | ------ |
| 6.2.1 | Registration eligibility (manufacturing, import, turnover thresholds)?                                                           | `NewRegSstSalesTaxCounter`                      | **No active numeric eligibility gate** in counter/portlet — Form 2 requires non-blank **annual taxable sales value** only (`checkForm2NContinue`). Commented-out **RM 500,000 tourism-tax trigger** exists in counter + portlet but is **disabled**. Manufacturing/import not coded as hard gates. |
| 6.2.2 | **Premises** — one primary + multiples? Link to tariff / finished-goods codes?                                                   | `premises_table.jsp`, `tariff_code_pop_up*.jsp` | **No `isPrimary` flag** on premises. **Registered address** on `SstInfo` (primary business address); **multiple additional `Premises`** on `Employer` via Form 1 table (optional at submit). **Tariff / finished-goods codes** on `SstInfo.sstTariffCodeSalesCategorySet` — **separate entity graph, not FK-linked to premises**; own popups/tables (`tariff_code_pop_up`, `finishgood_tariff_table`). Main-contract tariff list always required; sub-contract list required if `subContractWork` flag set. |
| 6.2.3 | Which of these are mandatory: `tourismTaxRegNo`, `incomeTaxRefNo`, `customAudRefNo`, `preRegNo`, `preRegName`, replacement date? | `NewRegSstSalesTaxForm1Dto`                     | **Optional on Form 1** — no legacy validator marks them required. Pre-reg block is officer-toggled via `preRegDataConfirm`; fields saved when entered. Same on update/incomplete auto-reg sales/service Form 1. |
| 6.2.4 | Validation on **annual taxable sales value** and **date sale value taxable**?                                                    | `SstInfoDto`                                    | **Form 2 mandatory** for sales+service (new reg + incomplete auto-reg). Audit WS: `annualTaxValue` required for sales/service only; `dateSaleValTaxGoods` not in WS. Incomplete listing uses `dateSaleValTaxGoods`+`manComDate` only (not annual value). |
| 6.2.5 | **Subcontract work** flag — routing or approval impact?                                                                          | `SstInfoDto.subContractWork`                    | **Validation only, no routing.** If flag set, sub-contract tariff list required on save; main-contract list always required. No separate approval path in counter/portlet code. |
| 6.2.6 | **Customs pre-registration** auto-populate (`preRegDataConfirm`)?                                                                | Form 1 Part A                                   | **No auto-populate.** Manual entry only. BRN search loads ASSIST employer data only — no Customs/JKDM WS. |




### 6.3 Tourism tax (`REG_SST_TOURISM_TAX`, `-CO-`)


| #     | Question                                                                 | Legacy reference                            | Answer |
| ----- | ------------------------------------------------------------------------ | ------------------------------------------- | ------ |
| 6.3.1 | Eligibility: accommodation providers only?                               | `NewRegSstTourismTaxCounter`                | **Accommodation-centric by design, not a coded hard gate.** Flow built around accommodation premises (type, rating, room count). No explicit “accommodation provider only” check in counter. Tourism can also be **auto-triggered** from parent service/sales when accommodation service group `1000L` + annual tax sale > RM500k (`RegUtility.checkAccomodationServiceAnnualTaxSaleLimit`) — trigger code exists; sales Form 2 RM500k check is **commented out/disabled**. MSIC hard-set to `656L` on submit. **Defer Phase 1 pilot** (§4.6 — sales + service first). |
| 6.3.2 | **MOTAC reg no** — mandatory? Validation source?                         | `SstInfo.motacRegNo`                        | **Optional** — Form 1 `motacRegistrationNo` not required; no MOTAC validation in `validateCommonSstForm1` tourism branch. Persisted to **`Employer.motacRegNo`** (tourism counter); `SstInfo.motacRegNo` used mainly in update DTOs. **No external MOTAC registry lookup** in registration module. |
| 6.3.3 | **Accommodation type / rating / room count** — reference data and rules? | `accommodation_premises_pop_up.jsp`         | Ref tables: **`AccomodationType`**, **`Rating`** via `RegOptionImpl.getAccommodationTypeOptionMap()` / `getAccommodationRatingOptionMap()`. Popup: type + rating **required**; `numOfRoom` numeric (max 14). Form 2: **≥1 accommodation premise mandatory** (`accomodation_premises_list_is_empty`). Stored on `AccommodationPremises` → `SstInfo.accommodationPremisesSet`. |
| 6.3.4 | Cross-check with tourism reg no captured on sales-tax Form 1?            | `NewRegSstSalesTaxForm1Dto.tourismTaxRegNo` | **No cross-check.** Sales Form 1 `tourismTaxRegNo` is **optional** informational field → `Employer.tourTaxRegNo` / `SstInfo.tourTaxRegNo`. Auto-populated from existing employer on BRN search only; not validated against MOTAC or tourism SMK. |
| 6.3.5 | Contact person logic shared with service tax — confirm?                  | tourism `contact_person_details_pop_up.jsp` | **Same UI pattern, separate data.** Both use `saveTempContactPersonRecord` keyed by tax-specific SMK column (`serviceTaxSmkRegNo` vs `tourismTaxSmkRegNo`). Separate `TempPersonal` rows per tax type — not shared across service and tourism registrations. |




### 6.4 Digital tax (`REG_SST_DIGITAL_TAX`, `-CD-`)


| #     | Question                                                          | Legacy reference                    | Answer |
| ----- | ----------------------------------------------------------------- | ----------------------------------- | ------ |
| 6.4.1 | Scope: foreign digital service provider, local platform, or both? | `NewRegSstDigitalTaxCounter`        | **No foreign/local/both flag** in code. Scope captured as **8 digital service type checkboxes** (`DigitalServiceTypeEnum`: software/apps, music/e-book/film, ads/platform, search/social, database/hosting, internet telecom, online training, others) on Form 2 + begin-operation date + total DS value. **Defer Phase 1 pilot** (§4.6). |
| 6.4.2 | **Authorised personnel** — required fields and count?             | `authorised_personnel_details*.jsp` | **≥1 required** on Form 1 (`hasDirectorOwnerList` / `director_owner_list_is_empty` — same mechanism as sales/service directors). Popup required: **name, identity type, identity no, email, designation**. Stored as `DirectorOwner` → `Employer` (despite “authorised personnel” label). |
| 6.4.3 | **Digital services** list — structure of `digitalServiceSet`?     | `SstInfo.digitalServiceSet`         | `Set<DigitalService>` on `SstInfo`; each row FK to **`DigitalServiceType`** ref + links to `employer` and `sstInfo`. Draft: `TempDigitalService` on `TempSstInfo`; Form 2 checkboxes create rows via `NewRegSstDigitalTaxCounter.saveTempDigitalType()`. |
| 6.4.4 | `websiteAddress` — mandatory?                                     | `SstInfo.websiteAddress`            | **Yes** — mandatory on digital Form 1 (`validateCommonSstForm1` REG_SST_DIGITAL_TAX branch + JSP required). Counter sanitizes invalid portal/localhost URLs (`sanitizeWebsiteAddressForSave`). |
| 6.4.5 | Cross-check with JKDM digital-tax registry or income tax?         |                                     | **None** — no JKDM or income-tax registry lookup in registration module. Manual entry only. |




### 6.5 DPSP tax (`REG_SST_DPSP_TAX`, `-CT-`)


| #     | Question                                                                                     | Legacy reference        | Answer |
| ----- | -------------------------------------------------------------------------------------------- | ----------------------- | ------ |
| 6.5.1 | Confirm **DPSP = Digital Platform Service Provider** under SST — legal scope used by PERKESO |                         | **Yes in UI/reg typing** — section title “Platform Digital Service Provider” (`reg.title_section.part_b_details_of_platform_digital_service_provider`); reg type `NEW_APPLICATION_DPSP_TAX`; SMK prefix `-CT-` on `dpspTaxSmkRegNo`. No separate legal-definition class beyond labels + form fields. **Defer Phase 1 pilot** (§4.6). |
| 6.5.2 | `operationDateInMalaysia` vs `businessComDate` — definitions and mandatory rules?            | `SstDpspTaxForm2Dto`    | Form 2 exposes **`operationDateInMalaysia` only**; counter persists it as **`TempSstInfo.businessComDate`** (`NewRegSstDpspTaxCounter.checkForm2NContinue`). **Both mandatory:** operation date + financial year end month (`validateSstDpspTaxForm2SaveCont`; FYE `-1` rejected). No separate `businessComDate` field on DPSP Form 2 UI. |
| 6.5.3 | Platform-specific data (app name, URL, commission model) — captured in ASSIST or JKDM only?  | DPSP form Part A/B JSPs | **Minimal in ASSIST** — Part A: business/address, `websiteAddress`, email; Part B: FY end month + operation date in Malaysia + applicant/declaration. **No platform list, app name, URL, or commission model fields** in DPSP JSPs/counter — section title only. Any platform registry detail would be JKDM-side or future integration. |
| 6.5.4 | Can one entity register **both digital tax and DPSP**, or mutually exclusive?                |                         | **Both allowed.** Separate nullable SMK columns (`digitalTaxSmkRegNo`, `dpspTaxSmkRegNo`) on `SstInfo`; duplicate check is **per tax type** (`RegUtility.isRegisteredTaxPayerByTaxType`). No mutual-exclusion logic between digital and DPSP. |


---



## 7. Update taxpayer (sections 1200–1204)

For **each** tax type:


| #   | Question                                                                                                  | Answer                          |
| --- | --------------------------------------------------------------------------------------------------------- | ------------------------------- |
| 7.1 | Which fields are editable after approval?                                                                 | **All Form 1/2 fields** open in update wizard per tax type; changes diffed vs production into `UpdatedItem` rows (`UpdateTaxPayerForm1Dto.prepareActualUpdatedItem` / `prepareTempUpdatedItem`). Child data editable via popups: directors (sales/service/digital), premises + tariff (sales), service-type codes (service), accommodation premises (tourism), contact persons (sales/service/tourism), authorised personnel (digital). Same field sets as new reg per tax type; pre-reg/customs fields optional. |
| 7.2 | Does update create a **new case** (new SMK ref) or amend in place?                                        | **New workflow case every time** — `RegTypeEnum.NEW_APPLICATION_UPDATE`, new `caseRefNo` (`UpdateTaxPayerInfoCounter.prepareNewDraftForAction`). On approve, **merges changes into existing `Employer` / `SstInfo` in place** — does **not** issue new SMK for routine field updates. |
| 7.3 | **Search taxpayer** — search keys: BRN, SST no, SMK no, employer code?                                    | **SST Registration No, Tax Payer Name, BRN, Tax Payer SMK No** — `RegOptionImpl.getSearchTaxPayerOptionMap`. **Employer code not in tax-payer search** (employer code is on separate Search for Employer flow). |
| 7.4 | Rejection / incomplete — same employer resubmit path as `IN_QUERY`?                                       | **Yes for IN_QUERY** — `UPDATE_TAX_PAYER_INFO_PREVIEW_IN_QUERY_SUBMIT` sets `IN_QUERY` + query remark; employer/officer resubmits via same update flow. **Reject → REJECTED** (UO action). No separate “incomplete” flag on update — incomplete auto-reg is a different module (1205–1209). |
| 7.5 | Popups needed in Phase 1: service type, premises, directors, tariff, accommodation, authorised personnel? | **All wired** in `RegJspPathEnum` UPDATE_* paths. Per tax type: **Service** — service type + director + contact person; **Sales** — director + premises + tariff + contact person; **Tourism** — accommodation + contact person; **Digital** — authorised personnel; **DPSP** — no contact-person popup on update Form 2. Phase 1 should include popups matching each tax type’s new-reg surface. |


---



## 8. Incomplete auto-registration (sections 1205–1209)


| #   | Question                                                                  | Answer                     |
| --- | ------------------------------------------------------------------------- | -------------------------- |
| 8.1 | **Source** of incomplete records — JKDM feed, batch job, officer-created? | **Audit auto-reg web service** (`AutoRegTaxPayerWs` → `AutoRegTaxPayerLibServiceImpl`) — not JKDM batch feed. Creates employer + partial `SstInfo` with `isAutoRegistration=true`, `autoRegistrationSourceFrom="audit"`. **Staff officer** completes via incomplete listing portlet (`IncompleteAutoRegTaxCounter`, sections 1205–1209). |
| 8.2 | Per tax type: which forms differ from full new registration?              | **Same field sets per tax type** as new reg but tax-specific incomplete Form 1/2 JSPs (§2 table). Sales/service get extended Part A (customs/pre-reg). Shared preview includes premises/director/tariff blocks where applicable. Counter reuses `submitNewRegFormRecord` on approve. |
| 8.3 | Employer portal login — link case by email / BRN / SST no?                | **Staff-only completion** in legacy — officer selects employer from incomplete listing (employer id + audit `SstInfo` row). **No employer portal self-service** for incomplete auto-reg. Portal email→employer linking (`UserEmployerWs`) exists on **new reg approve**, not incomplete path. |
| 8.4 | Completion SLA and reminder notices?                                      | **No incomplete-auto-reg-specific SLA or reminders** in code. Generic registration reminder framework (`ReminderLibImpl`) exists module-wide — confirm with business if SLA/reminders are operational outside codebase. |
| 8.5 | Reference tables (tariff, service type) — same as full registration?      | **Yes — same ref tables:** `REF_SST_SERVICE_TYPE`, `SstServiceGroup`, `TariffCodeSalesType`, `AccomodationType`, `Rating`, etc. (`ref.pojo.*`). Completeness gate for listing: sales/service need `manComDate` + `dateSaleValTaxGoods`; tourism/digital/dpsp need `applicantPersonal` (`EmployerImpl.isAuditAutoRegSstDataCompleteForTaxType`). |


---



## 9. Data model and replatform decisions


| #   | Question                                                                                                        | Legacy hint                  | Answer |
| --- | --------------------------------------------------------------------------------------------------------------- | ---------------------------- | ------ |
| 9.1 | Keep **one** `SstInfo` **row per employer** with per-tax SMK columns, or normalize to child table?              | **Many `SST_INFO` rows per employer** (see §4.5): one row per SST registration; sales allows multiple rows each with own `salesTaxSmkRegNo`; service/tourism/digital/DPSP max one active row each per employer. Replatform: `sst_info.employer_id` FK, not one-row-per-employer with all SMK columns. |
| 9.2 | `SstStatusInfo` / `SstStatusTypeEnum` — status **per tax type** or global?                                      | **Per tax type on each `SstInfo` row** — `SstStatusInfo.taxTypeId` + status Active/Cancel (`SstStatusTypeEnum`). New-reg submit creates one ACTIVE status row per tax type (`AbstractRegCounter.submitSstStatusInfo`). Discontinue updates status for **selected tax type only**. Not a single global employer-level SST flag. |
| 9.3 | Migrate reference data: financial year-end month, accommodation type, rating, tariff codes, service categories? | Ref data = Hibernate **`ref.pojo.*`** entities (`SstServiceType`, `TariffCodeSalesType`, `AccomodationType`, `Rating`, `SstServiceGroup`, etc.). **No Liquibase/seed scripts in legacy repo** — migrate from legacy DB or existing ref maintenance. FYE month is a form field (1–12), not a separate ref table. |
| 9.4 | **Area code** for SST reg no — same `reg_area_code` as SOCSO employer code?                                     | **Same lookup:** `AreaCodeLibImpl.findAreaCodeByBranchIdPostcode(branchId, postCode)`. **SST registration no:** `{areaCode}-{MM}{YYYY}-{8-digit}` (`SstRegistrationNo` — doc hint `SstRegistrationNoGenerator` is misnamed). **SMK no:** `{areaCode}-{CP/CJ/CO/CD/CT-}{8-digit}/{year}` (`SmkNo`). Area code source shared with SOCSO employer code generation. |
| 9.5 | Must replatform preserve legacy case refs and SMK numbers 1:1 for migration?                                    | **Workshop decision** — legacy code has no dedicated migration preservation layer. Recommend **1:1 preserve** `smkRegNo`, per-tax SMK columns, SST registration number, and historical `caseRefNo` for in-flight and completed cases. New cases post-cutover use NS3 generators with same formats. |
| 9.6 | Liquibase: new `sst_info` schema in `assist-registration` or separate module?                                   | **Legacy:** no Liquibase in repo — schema via Hibernate entity mappings only. **NS3 recommendation:** `sst_info` + child tables (status, tariff, accommodation, service category, etc.) in **`assist-registration`** alongside employer/case entities — keeps registration bounded context unified for Phase 1. |


---



## 10. Suggested workshop order

```
Scope & priority (§4)
    → Service vs sales entry point (§4.2, §6.1)
    → Shared Part A/B/C rules (§5)
    → Per-tax specifics (§6)
    → Update & incomplete flows (§7–§8)
    → JKDM / SST02 integration (§5.11)
    → Numbering & letters (§1, §5.1, §5.10)
    → Data model sign-off (§9)
```

---



## 11. Email-ready summary (copy/paste)

> For SST replatforming we need confirmation on:
>
> 1. Phase 1 tax types and flows (new / update / incomplete / discontinue)
> 2. Whether **service tax** remains under standard `REG_NEW_REG` (200)
> 3. Mandatory fields and validations per tax (Part A/B/C, premises, tariff, directors)
> 4. Tourism-specific rules (MOTAC, accommodation premises, rating)
> 5. Digital vs **DPSP** scope and unique fields
> 6. JKDM approval and **SST02** outstanding-amount checks
> 7. ID formats: SST reg no vs SMK prefixes (`-CP-` / `-CJ-` / `-CO-` / `-CD-` / `-CT-`)
> 8. Whether one employer can hold multiple concurrent SST registrations
>
> Reference: legacy ASSIST sections 1100–1109, 1200–1209; replatform checklist at `docs/sst-discovery-questions.md`.

---



## 12. Sign-off

Complete after SST replatform workshop. Names and dates are **not in legacy code** — capture from business/JKDM/PERKESO stakeholders at sign-off meeting.


| Role                     | Name | Date | Notes |
| ------------------------ | ---- | ---- | ----- |
| Business owner (SST)     | *TBD* |      | Confirms §4 scope, §5 field rules, §6 per-tax behaviour, Phase 1 pilot (sales + service) |
| JKDM liaison             | *TBD* |      | Confirms UO-as-JKDM-proxy interpretation (§4.4), MOTAC/tourism rules if tourism in scope, no external approval API required for parity |
| PERKESO registration SME | *TBD* |      | Confirms workflow roles (RO/UO), letters, SST02 discontinue gate, incomplete auto-reg process |
| Replatform tech lead     | *TBD* |      | Confirms §9 data model, migration 1:1 policy, NS3 module placement |


### What remains open after this document

| Item | Status |
| ---- | ------ |
| §6.3–6.5 tourism / digital / DPSP | **Documented from legacy** — **deferred from Phase 1 pilot** per §4.6 (sales + service first); implement when pilot expands |
| §8.4 incomplete SLA / reminders | **Not in codebase** — confirm with operations whether reminders run outside ASSIST |
| §9.5 migration 1:1 cutover | **Workshop decision** — recommend preserve SMK, SST reg no, case refs; no legacy preservation layer in code |
| §12 sign-off names | **Pending workshop** — fill table above |
| External JKDM API (future) | **Not required for legacy parity** — only if business wants real-time JKDM integration beyond UO workflow + employer messaging |


