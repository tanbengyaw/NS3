package my.gov.perkeso.assist.registration.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AreaCodeLookupService {

    private final JdbcTemplate jdbcTemplate;

    public String findAreaCodeByPostCodeAndBranchId(final String postCode, final Long branchId) {
        if (postCode == null || postCode.isBlank()) {
            throw new IllegalArgumentException("postCode cannot be blank");
        }
        if (branchId == null) {
            throw new IllegalArgumentException("branchId cannot be null");
        }

        final String areaCode = jdbcTemplate.query(
                "SELECT area_code FROM registration.reg_area_code "
                        + "WHERE branch_id = ? AND postcode = ? AND is_active = TRUE LIMIT 1",
                rs -> rs.next() ? rs.getString(1) : null, branchId, postCode.trim());
        if (areaCode != null) {
            return areaCode;
        }
        return jdbcTemplate.query(
                "SELECT area_code FROM registration.reg_area_code "
                        + "WHERE postcode = ? AND is_active = TRUE ORDER BY id LIMIT 1",
                rs -> rs.next() ? rs.getString(1) : null, postCode.trim());
    }

    /**
     * Strict lookup used when a missing mapping must fail fast (e.g. SOCSO employer code).
     */
    public String requireAreaCodeByPostCodeAndBranchId(final String postCode, final Long branchId) {
        final String areaCode = findAreaCodeByPostCodeAndBranchId(postCode, branchId);
        if (areaCode == null || areaCode.isBlank()) {
            throw new IllegalArgumentException(
                    "No area code for postcode " + postCode + " and branch " + branchId);
        }
        return areaCode;
    }

    /**
     * Customs control-station label for SST letters (legacy {@code RegAreaPostcodeImpl#findAreaNameByPostcode}).
     */
    public String findCustomsAreaNameByPostcode(final String postCode) {
        if (postCode == null || postCode.isBlank()) {
            return null;
        }
        return jdbcTemplate.query("""
                SELECT area_name FROM registration.reg_area_code
                WHERE postcode = ? AND is_active = TRUE AND area_name IS NOT NULL
                ORDER BY id
                LIMIT 1
                """, rs -> rs.next() ? rs.getString(1) : null, postCode.trim());
    }
}
