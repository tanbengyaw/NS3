package my.gov.perkeso.assist.registration.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReferenceDataIntegrityService {

    private final JdbcTemplate jdbcTemplate;

    public int countOfficePostcodesMissingAreaCode() {
        final Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM reference.ref_postcode_office po
                LEFT JOIN registration.reg_area_code ac
                  ON ac.branch_id = po.branch_id
                 AND ac.postcode = po.postcode
                 AND ac.is_active = TRUE
                WHERE ac.id IS NULL
                """, Integer.class);
        return count != null ? count : 0;
    }

    public boolean isReferenceDataHealthy() {
        return countOfficePostcodesMissingAreaCode() == 0;
    }
}
