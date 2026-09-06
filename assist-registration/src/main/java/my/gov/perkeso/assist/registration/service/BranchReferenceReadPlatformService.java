package my.gov.perkeso.assist.registration.service;

import lombok.Builder;
import lombok.Getter;
import my.gov.perkeso.assist.registration.data.RefOptionData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BranchReferenceReadPlatformService {

    private final JdbcTemplate jdbcTemplate;

    public BranchReferenceReadPlatformService(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RefOptionData> retrieveAllBranches() {
        return jdbcTemplate.query("""
                SELECT id, name
                FROM reference.ref_branch
                ORDER BY sort_order, name
                """, (rs, rowNum) -> RefOptionData.builder()
                .id(rs.getLong("id"))
                .label(rs.getString("name"))
                .build());
    }

    public BranchLetterData retrieveBranchForLetter(final Long branchId) {
        if (branchId == null) {
            return null;
        }
        return jdbcTemplate.query("""
                SELECT b.id, b.name, b.address_line1, b.address_line2, b.address_line3,
                       b.postcode, b.city_name, b.phone, b.fax, s.name AS state_name
                FROM reference.ref_branch b
                LEFT JOIN reference.ref_state s ON s.id = b.state_id
                WHERE b.id = ?
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
                    .phone(rs.getString("phone"))
                    .fax(rs.getString("fax"))
                    .stateName(rs.getString("state_name"))
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
        private final String phone;
        private final String fax;
        private final String stateName;
    }
}
