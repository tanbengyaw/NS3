package my.gov.perkeso.assist.registration.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.data.PostcodeOptionData;
import my.gov.perkeso.assist.registration.data.RefOptionData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressReferenceReadPlatformService {

    private final JdbcTemplate jdbcTemplate;

    public List<RefOptionData> retrieveStates() {
        return jdbcTemplate.query("""
                SELECT id, name
                FROM reference.ref_state
                ORDER BY sort_order, name
                """, (rs, rowNum) -> RefOptionData.builder().id(rs.getLong("id")).label(rs.getString("name")).build());
    }

    public List<RefOptionData> retrieveCities(final Long stateId) {
        if (stateId == null) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT id, name
                FROM reference.ref_city
                WHERE state_id = ?
                ORDER BY name
                """, (rs, rowNum) -> RefOptionData.builder().id(rs.getLong("id")).label(rs.getString("name")).build(),
                stateId);
    }

    public List<PostcodeOptionData> retrievePostcodes(final Long stateId, final Long cityId) {
        final StringBuilder sql = new StringBuilder("""
                SELECT p.postcode, s.id AS state_id, s.name AS state_name,
                       c.id AS city_id, c.name AS city_name
                FROM reference.ref_postcode p
                JOIN reference.ref_state s ON s.id = p.state_id
                JOIN reference.ref_city c ON c.id = p.city_id
                WHERE 1 = 1
                """);
        final List<Object> args = new ArrayList<>();
        if (stateId != null) {
            sql.append(" AND p.state_id = ?");
            args.add(stateId);
        }
        if (cityId != null) {
            sql.append(" AND p.city_id = ?");
            args.add(cityId);
        }
        sql.append(" ORDER BY p.postcode");
        return jdbcTemplate.query(sql.toString(), new PostcodeRowMapper(), args.toArray());
    }

    public List<RefOptionData> retrieveOfficeLocations(final String postcode) {
        if (postcode == null || postcode.isBlank()) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT b.id, b.name
                FROM reference.ref_postcode_office po
                JOIN reference.ref_branch b ON b.id = po.branch_id
                WHERE po.postcode = ?
                ORDER BY po.sort_order DESC, b.sort_order, b.name
                """, (rs, rowNum) -> RefOptionData.builder().id(rs.getLong("id")).label(rs.getString("name")).build(),
                postcode.trim());
    }

    private static final class PostcodeRowMapper implements RowMapper<PostcodeOptionData> {

        @Override
        public PostcodeOptionData mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final String postcode = rs.getString("postcode");
            final String cityName = rs.getString("city_name");
            final String stateName = rs.getString("state_name");
            return PostcodeOptionData.builder().postcode(postcode)
                    .label(postcode + " — " + cityName + ", " + stateName).stateId(rs.getLong("state_id"))
                    .stateName(stateName).cityId(rs.getLong("city_id")).cityName(cityName).build();
        }
    }
}
