-- Export ASSIST legacy reference data for NS3 replatform (PostgreSQL / Oracle dialect may need tweaks).
-- Run against legacy ASSIST DB, save results as CSV, then load via Liquibase loadData (see docs/reference-data-load.md).
--
-- Legacy tables (from AreaCodeLibHome / AreaPostcode):
--   BASE_REF_AREA_CODE, BASE_REF_AREA, BASE_REF_BRANCH, area_postcode (postcode mapping)

-- 1) Branch master -> reference.ref_branch
SELECT b.id,
       b.name,
       b.sort_order,
       b.state_id,
       b.phone,
       b.fax,
       b.address_line1,
       b.address_line2,
       b.address_line3,
       b.postcode,
       b.city_name
FROM base_ref_branch b
WHERE b.is_deleted = 0
ORDER BY b.id;

-- 2) Postcode -> office (branch) -> reference.ref_postcode_office
SELECT DISTINCT ap.postcode,
       a.branch_id,
       10 AS sort_order
FROM area_postcode ap
JOIN base_ref_area a ON a.id = ap.area_id
WHERE ap.is_deleted = 0
  AND a.is_deleted = 0
  AND a.is_activated = 1
ORDER BY ap.postcode, a.branch_id;

-- 3) Area code by branch + postcode -> registration.reg_area_code
SELECT a.branch_id,
       ap.postcode,
       ac.value AS area_code,
       a.name   AS area_name,
       1        AS is_active
FROM base_ref_area_code ac
JOIN base_ref_area a ON a.id = ac.area_id
JOIN area_postcode ap ON ap.area_id = a.id
WHERE ac.is_deleted = 0
  AND ac.is_activated = 1
  AND a.is_deleted = 0
  AND a.is_activated = 1
  AND ap.is_deleted = 0
ORDER BY a.branch_id, ap.postcode;

-- 4) Validation: office postcodes missing area code (should return zero rows before cutover)
SELECT po.postcode, po.branch_id
FROM reference.ref_postcode_office po
LEFT JOIN registration.reg_area_code ac
  ON ac.branch_id = po.branch_id
 AND ac.postcode = po.postcode
 AND ac.is_active = TRUE
WHERE ac.id IS NULL;
