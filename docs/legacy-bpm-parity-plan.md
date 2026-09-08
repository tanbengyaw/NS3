# Legacy BPM parity — architecture plan (future reference)

**Status:** Planning only — **not in Phase 1 scope**.  
**Context:** NS3 today implements **behavioral** workflow parity in Java (`RegistrationCaseSubmitRouter`, inbox SQL filters) without a BPM engine. This document describes how to achieve **full legacy BPM parity** across registration, collection, compound, audit, and other ASSIST modules if that becomes a requirement.

**Related today:**

| Item | Location |
|------|----------|
| Submit routing (code-only) | [submit-routing.md](./submit-routing.md), `RegistrationCaseSubmitRouter` |
| Case “routed to” display | `RegistrationCaseRouting`, `case-routing.util.ts` |
| Phase 1 scope note | Root [README.md](../README.md) — “jBPM / full ASSIST workflow parity” |

**Legacy reference checkout:** `assist_new_merge_only` (PERKESO ASSIST monorepo).

---

## 1. What “full BPM parity” means

| Level | What you get | NS3 today |
|-------|----------------|-----------|
| **Behavioral parity** | Same status transitions, role queues, approve/query/reject outcomes | **Yes** (registration slice) |
| **Full BPM parity** | jBPM/Camunda process instances, task inbox from engine, `process_instance_id`, task history, round-robin assignment | **No** |

Full parity means a user’s **Officer / UO inbox** is backed by **workflow tasks**, not only by filtering `reg_general_info.status`.

---

## 2. How legacy ASSIST works

Every major module follows the same pattern:

```
Counter / API submit
  → Module WorkflowDriver (switch on section)
  → *WorkflowServiceImpl (prepare routing rules)
  → WorkflowWs (shared base layer)
  → WorkflowLibImpl → jBPM / KIE
  → BPMN process (separate workflow Maven module)
```

### Per-module drivers (legacy)

| Module | Driver | Workflow service examples |
|--------|--------|---------------------------|
| **Registration** | `RegWorkflowDriver` | `NewRegWorkflowServiceImpl`, `UpdateTaxPayerWorkflowServiceImpl`, `DiscontinueTaxWorkflowServiceImpl`, SST variants |
| **Collection** | `CollectionTaskWorkflowImpl` | start / continue / end workflow per collection task |
| **Compound** | `CompWorkflowDriver` | `PreCompWorkflowServiceImpl`, `CompoundWorkflowServiceImpl`, `CompoundAppealWorkflowServiceImpl`, `MeetingWorkflowServiceImpl` |
| **Audit** | audit services | separate from registration; feeds registration via `AutoRegTaxPayerWs` |

### Shared integration point

- **`WorkflowWs`** (`base-ws`) — `startNewWorkflow`, `continueWorkflow`, `endWorkflow`, task queries
- **`TaskData`** — target role(s), branch, `ProcessIdEnum`, `TaskActionEnum`, `TaskStatusEnum`, `WorkflowUrlTypeEnum`, `ModuleEnum`
- **`process_instance_id`** stored on the business record
- BPM runs only when environment profile / flag is set (`bmpEnv` in legacy)

### BPMN assets (separate repos inside legacy monorepo)

Legacy keeps BPMN in dedicated workflow modules under `assist/workflow/`:

| Workflow module | Approx. BPMN count | Examples |
|-----------------|-------------------|----------|
| `registration-workflow` | ~16 | `newRegistrationViaOtc.bpmn`, `updateEmployerInformation.bpmn` |
| `collection-workflow` | ~16 | `adjustmentPaymentAmount.bpmn`, `eftPayment.bpmn` |
| `compound-workflow` | ~12 | `compoundInternal.bpmn`, `compoundAppeal.bpmn`, `compoundMeeting.bpmn` |
| `audit-workflow` | ~4 | `newAudit.bpmn`, `auditCreate.bpmn` |
| Others | ~100+ | contribution, benefit, eduloan, debt recovery, legal, detection, … |

**Total:** ~167 BPMN files across ASSIST — multi-year rollout if all are ported.

---

## 3. Target architecture in NS3

### 3.1 New shared module: `assist-workflow`

One platform module replaces `WorkflowWs` for all domains:

```
assist-workflow/
  api/          WorkflowFacade (start / continue / end / query tasks)
  domain/       TaskCommand, WorkflowContext (module, section, branch, roles)
  engine/       Camunda or Flowable adapter (recommended)
  inbox/        Unified task query API
  resources/    Imported BPMN from legacy assist/workflow/*
```

### 3.2 Engine choice

| Option | Pros | Cons |
|--------|------|------|
| **Keep jBPM / KIE** | Strictest parity with legacy BPMN and runtime | Old stack; difficult with Java 17+ / Spring Boot |
| **Camunda / Flowable** (recommended) | Spring-native, easier operations | Must validate/import legacy BPMN; some processes may need adjustment |
| **Code-only (current NS3)** | Simple, fast to ship | Not BPM parity — no shared task engine or process history |

For **real** parity, prefer **Camunda or Flowable embedded in `assist-provider`**, importing legacy BPMN module-by-module.

### 3.3 Per-module drivers — do not centralize business rules

Each replatformed module keeps its **own** driver (same as legacy):

```
assist-registration → RegistrationWorkflowDriver
assist-collection   → CollectionWorkflowDriver
assist-compound     → CompoundWorkflowDriver
assist-audit        → AuditWorkflowDriver
```

Each driver:

1. Switches on **section**
2. Picks the right `*WorkflowService`
3. Builds `TaskData` (target role, branch, task action, URL type)
4. Calls `WorkflowFacade.startNewWorkflow()` or `continueWorkflow()`
5. Persists returned **`process_instance_id`** on the case row

### 3.4 Cross-module integration stays at API boundaries

BPM parity does **not** mean one mega-process across modules:

- **Audit** → `AutoRegTaxPayerWs` → registration incomplete case (separate processes, linked by employer/ref)
- **Collection** → SST02 outstanding check before discontinue (read API, not shared workflow)
- **Compound** → may spawn from audit/detection; parent/child `process_instance_id` where legacy uses it

**Integrate at boundaries; own workflow per module.**

---

## 4. Migrating registration (bridge from current NS3)

Registration is the best first module — business rules already exist in Java.

| Step | Action |
|------|--------|
| A | Add `process_instance_id` to `reg_general_info` (Liquibase) |
| B | Extract `RegistrationCaseSubmitRouter` / write-service rules into `RegistrationWorkflowService` classes (one per section group), mirroring legacy `*WorkflowServiceImpl` |
| C | Wire submit / approve / query / reject to `WorkflowFacade` **and** update status column |
| D | **Dual-run period:** keep status + BPM task; compare inbox results legacy vs NS3 |
| E | Switch Officer / UO inbox to workflow task query when stable |

Keep a `workflow.enabled` feature flag (like legacy `bmpEnv`) so dev/tests can run without the engine.

---

## 5. BPMN import strategy

Copy from legacy `assist/workflow/` into NS3:

```
assist-workflow-resources/
  registration/   newRegistrationViaOtc.bpmn, …
  collection/     adjustmentPaymentAmount.bpmn, …
  compound/       compoundInternal.bpmn, …
  audit/          newAudit.bpmn, …
```

- Map legacy `ProcessIdEnum` → process definition key
- Deploy one **deployment unit per `ModuleEnum`** (REG, COLLECTION, COMPOUND, AUDIT) — same idea as legacy `kmodule.xml` / KIE deployment descriptor
- **Import and fix** — do not redraw all 167 BPMN unless the engine cannot load the XML

---

## 6. Unified inbox UI (Angular)

Replace siloed per-module inboxes with one task API:

```
GET /api/v1/workflow/tasks?module=REG&branchId=2&role=UO
```

Suggested columns: case ref, module, section, employer, **routed to**, task status, deep link.

Legacy `WorkflowUrlTypeEnum` → Angular routes, e.g.:

| URL type | Route |
|----------|-------|
| `REG_NEW_REG` | `/registration/:caseId` |
| `COMP_COMPOUND` | `/compound/:caseId` |

The **Base → Case routing listing** screen can become a debug/admin view on the same underlying task data.

---

## 7. Proving parity (tests)

For each section / process:

1. Port legacy `TestWorkflow*` characterization tests where they exist
2. Golden-path E2E: same user + action → same **target role**, **branch**, **task status**
3. Side-by-side UAT: legacy ASSIST inbox vs NS3 inbox for the same case ref

**Parity = task assignment matches**, not only final `APPROVED` / `REJECTED`.

---

## 8. Recommended rollout order

```
Phase 0: assist-workflow foundation (engine, task API, inbox shell)
    ↓
Phase 1: Registration BPMN (SST + SOCSO — start with one process, e.g. newRegistrationViaOtc)
    ↓
Phase 2: Collection (adjustments, EFT, receipts)
    ↓
Phase 3: Compound (proposed compound, appeal, meeting)
    ↓
Phase 4: Audit (create + supervisor) + cross-module triggers to registration
    ↓
Phase 5+: Contribution, benefit, eduloan, debt recovery, legal, … (~100+ remaining BPMN)
```

| Phase | Scope | Rationale |
|-------|--------|-----------|
| 0 | `assist-workflow` platform | Foundation for all modules |
| 1 | Registration | Highest reuse; validates platform; rules already coded in NS3 |
| 2 | Collection | High volume; clear `CollectionTaskWorkflowImpl` pattern |
| 3 | Compound | Many sub-flows but isolated domain |
| 4 | Audit | Smaller BPMN set; ties into registration incomplete auto-reg |
| 5+ | Remaining modules | Long tail |

---

## 9. What not to do

1. **One global workflow module with all business rules** — legacy never did this; drivers stay per domain.
2. **Rewrite all BPMN by hand** — import from legacy and fix only where needed.
3. **Big-bang cutover** — dual-run status column + BPM tasks until inbox parity is proven.
4. **Skip `process_instance_id`** — without it you lose audit trail, task history, and round-robin assignment.

---

## 10. Decision summary

| Need | Approach |
|------|----------|
| Same outcomes, faster delivery | Keep current NS3 code routing (`RegistrationCaseSubmitRouter`, SQL inbox) |
| True legacy BPM parity | `assist-workflow` + Camunda/Flowable + per-module drivers + imported BPMN + unified task inbox |
| Rough effort | Platform ~2–3 months; **full ASSIST parity = years** (same scale as replatforming all modules) |

---

## 11. First concrete step (when approved)

**Phase 0 spike:**

1. Scaffold `assist-workflow` Gradle module
2. Embed Camunda or Flowable in `assist-provider`
3. Import **one** registration BPMN (e.g. `newRegistrationViaOtc.bpmn`)
4. Wire NS3 sales-tax submit to create a real `process_instance_id` while keeping today’s status column as fallback
5. Add one E2E test comparing task assignment to legacy behavior for that path

---

## 12. Legacy file index (quick lookup)

| Legacy path | Purpose |
|-------------|---------|
| `…/registration/service/RegWorkflowDriver.java` | Registration section → workflow service routing |
| `…/registration/service/impl/NewRegWorkflowServiceImpl.java` | Example: RO / PKR_BO routing for new reg |
| `…/base/impl/WorkflowWs.java` | Shared workflow API |
| `…/compound/lib/service/CompWorkflowDriver.java` | Compound section routing |
| `…/collection/impl/CollectionTaskWorkflowImpl.java` | Collection task workflow |
| `assist/workflow/registration-workflow/…/bpm/*.bpmn` | Registration process definitions |
| `assist/workflow/collection-workflow/…/bpmn/*.bpmn` | Collection process definitions |
| `assist/workflow/compound-workflow/…/*.bpmn` | Compound process definitions |
| `assist/workflow/audit-workflow/…/bpmn/*.bpmn` | Audit process definitions |

---

*Document created from architecture discussion (2026-09-07). Review before starting Phase 0.*
