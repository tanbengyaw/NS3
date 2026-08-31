package my.gov.perkeso.assist.registration.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AreaCodeLookupService {

    private static final String AREA_CODE_F75 = "F75";
    private static final Long TAPAH_BRANCH_ID = 10L;
    private static final String POSTCODE_39100 = "39100";
    private static final String POSTCODE_39200 = "39200";

    private final JdbcTemplate jdbcTemplate;

    public String findAreaCodeByPostCodeAndBranchId(final String postCode, final Long branchId) {
        if (postCode == null || postCode.isBlank()) {
            throw new IllegalArgumentException("postCode cannot be blank");
        }
        if (branchId == null) {
            throw new IllegalArgumentException("branchId cannot be null");
        }

        if ((POSTCODE_39100.equals(postCode) || POSTCODE_39200.equals(postCode))
                && TAPAH_BRANCH_ID.equals(branchId)) {
            return AREA_CODE_F75;
        }

        return jdbcTemplate.query(
                "SELECT area_code FROM registration.reg_area_code "
                        + "WHERE branch_id = ? AND postcode = ? AND is_active = TRUE LIMIT 1",
                rs -> rs.next() ? rs.getString(1) : null, branchId, postCode);
    }
}
