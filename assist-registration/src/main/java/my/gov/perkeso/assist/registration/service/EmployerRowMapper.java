package my.gov.perkeso.assist.registration.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import my.gov.perkeso.assist.registration.data.EmployerData;
import org.springframework.jdbc.core.RowMapper;

final class EmployerRowMapper implements RowMapper<EmployerData> {

    static final EmployerRowMapper INSTANCE = new EmployerRowMapper();

    private EmployerRowMapper() {
    }

    @Override
    public EmployerData mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        return EmployerData.builder().id(rs.getLong("id")).employerCode(rs.getString("employer_code"))
                .employerName(rs.getString("employer_name")).registrationNo(rs.getString("registration_no"))
                .serviceTypeId(rs.getObject("service_type_id") != null ? rs.getLong("service_type_id") : null)
                .pksBranchId(rs.getLong("pks_branch_id")).branch(rs.getBoolean("is_branch"))
                .msicId(rs.getObject("msic_id") != null ? rs.getLong("msic_id") : null)
                .contributionActive(rs.getBoolean("is_contribution_active"))
                .operationalStatus(rs.getString("status"))
                .createdDate(rs.getTimestamp("created_date") != null
                        ? rs.getTimestamp("created_date").toLocalDateTime()
                        : null)
                .build();
    }
}
