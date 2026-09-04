# Reference data — area codes, branches, postcodes

NS3 uses three linked tables for employer/SST code generation and the registration wizard:

| Table | Schema | Purpose |
|-------|--------|---------|
| `ref_branch` | `reference` | PERKESO office locations (PKS branch) |
| `ref_postcode_office` | `reference` | Maps business postcode → processing office |
| `reg_area_code` | `registration` | Area code prefix for employer code / SMK (`AreaCodeLookupService`) |

Legacy ASSIST equivalent: `AreaCodeLibImpl.findAreaCodeByBranchIdPostcode(branchId, postCode)`.

## Dev seed (current)

Liquibase migrations **0002–0024** seed a **dev subset**:

- **16 states** (complete)
- **~20 cities**, **~31 postcodes**
- **17 branches** (KL, Shah Alam, PJ, JB, Penang, Tapah + state capitals)
- **~31 postcode→office mappings**
- **Area code for every postcode–office pair** (0024 backfill)

After migration **0024**, every row in `ref_postcode_office` has a matching active `reg_area_code` row.

## Production load (full master)

Production requires export from legacy ASSIST (thousands of postcode mappings).

### Step 1 — Export from legacy DB

Use `scripts/export-legacy-reference-data.sql` against staging/production ASSIST.

Save query results as CSV:

```
assist-registration/src/main/resources/db/reference-data/
  ref_branch.csv
  ref_postcode_office.csv
  reg_area_code.csv
```

CSV headers must match Liquibase `loadData` columns (see templates in that folder).

### Step 2 — Add Liquibase load changeset

Example (add as `0025_reference_data_production_load.xml`, run once on prod):

```xml
<changeSet id="0025-load-reg-area-code" author="ns3" context="reference-data-full">
  <loadData file="db/reference-data/reg_area_code.csv"
            tableName="reg_area_code"
            schemaName="registration"
            separator=","
            relativeToChangelogFile="false">
    <column name="branch_id" type="NUMERIC"/>
    <column name="postcode" type="STRING"/>
    <column name="area_code" type="STRING"/>
    <column name="area_name" type="STRING"/>
    <column name="is_active" type="BOOLEAN"/>
  </loadData>
</changeSet>
```

Run with: `-Dspring.liquibase.contexts=reference-data-full` (or equivalent).

### Step 3 — Validate

```sql
-- Every office postcode must have area code
SELECT po.postcode, po.branch_id
FROM reference.ref_postcode_office po
LEFT JOIN registration.reg_area_code ac
  ON ac.branch_id = po.branch_id AND ac.postcode = po.postcode AND ac.is_active = TRUE
WHERE ac.id IS NULL;
```

Automated check: `ReferenceDataIntegrityTest` in `assist-registration`.

## API (frontend)

| Endpoint | Use |
|----------|-----|
| `GET /reference/address/states` | State dropdown |
| `GET /reference/address/cities?stateId=` | City dropdown |
| `GET /reference/address/postcodes?stateId=&cityId=` | Postcode dropdown |
| `GET /reference/address/office-locations?postcode=` | PKS branch after postcode selected |

If postcode has no office mapping, the wizard keeps the current branch default (Shah Alam id=2).

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| Approve fails: `No area code for postcode …` | Missing `reg_area_code` row | Add branch+postcode row or run 0024 backfill |
| Office dropdown empty | Missing `ref_postcode_office` | Add mapping for that postcode |
| Wrong branch auto-selected | Multiple offices for postcode | Highest `sort_order` wins |
| Letter ref `208/{areaCode}/…` wrong | Missing `area_name` on `reg_area_code` | Set `area_name` column |
