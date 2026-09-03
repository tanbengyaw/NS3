package my.gov.perkeso.assist.registration.service;

import lombok.Builder;
import lombok.Getter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class BranchReferenceReadPlatformService {

    private final JdbcTemplate jdbcTemplate;

    public BranchReferenceReadPlatformService(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BranchLetterData retrieveBranchForLetter(final Long branchId) {
        if (branchId == null) {
            return null;
        }
        return jdbcTemplate.query("""
                SELECT id, name, address_line1, address_line2, address_line3, postcode, city_name
                FROM reference.ref_branch
                WHERE id = ?
                """, rs -> {
            if (!rs.next()) {
                return null;
            }
            return BranchLetterData.builder()
                    .id(rs.getLong("id"))
                    .name(rs.getString("name"))
                    .addressLine1(rs.getString("address_line1"))
                    .addressLine2(rs.getString("address_line2"))
                    .addressLine3(rs.getString("address_line3"))
                    .postCode(rs.getString("postcode"))
                    .cityName(rs.getString("city_name"))
                    .build();
        }, branchId);
    }

    @Getter
    @Builder
    public static class BranchLetterData {
        private final Long id;
        private final String name;
        private final String addressLine1;
        private final String addressLine2;
        private final String addressLine3;
        private final String postCode;
        private final String cityName;
    }
}
