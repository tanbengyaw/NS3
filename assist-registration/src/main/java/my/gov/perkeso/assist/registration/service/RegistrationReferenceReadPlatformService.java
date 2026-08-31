package my.gov.perkeso.assist.registration.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.data.RefOptionData;
import my.gov.perkeso.assist.registration.data.TariffCodeSalesTypeData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegistrationReferenceReadPlatformService {

    private final JdbcTemplate jdbcTemplate;

    public List<RefOptionData> retrieveBusinessEntityTypes() {
        return jdbcTemplate.query("""
                SELECT id, name
                FROM reference.ref_business_entity
                ORDER BY sort_order, name
                """, (rs, rowNum) -> RefOptionData.builder().id(rs.getLong("id")).label(rs.getString("name")).build());
    }

    public List<RefOptionData> retrieveIdentificationTypes(final boolean directorFormOnly) {
        final String sql = directorFormOnly
                ? """
                        SELECT id, name
                        FROM reference.ref_identification_type
                        WHERE director_form_eligible = TRUE
                        ORDER BY sort_order, name
                        """
                : """
                        SELECT id, name
                        FROM reference.ref_identification_type
                        ORDER BY sort_order, name
                        """;
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> RefOptionData.builder().id(rs.getLong("id")).label(rs.getString("name")).build());
    }

    public List<TariffCodeSalesTypeData> searchTariffCodeSalesTypes(final String search) {
        final String term = search == null ? "" : search.trim();
        if (term.isEmpty()) {
            return List.of();
        }
        final String pattern = term.toUpperCase() + "%";
        return jdbcTemplate.query("""
                SELECT id, code, description
                FROM reference.ref_tariff_code_sales_type
                WHERE is_deleted = FALSE
                  AND LENGTH(code) = 13
                  AND UPPER(code) LIKE ?
                ORDER BY code
                LIMIT 50
                """, (rs, rowNum) -> TariffCodeSalesTypeData.builder()
                .id(rs.getLong("id"))
                .code(rs.getString("code"))
                .description(rs.getString("description"))
                .build(), pattern);
    }

    public Map<Long, TariffCodeSalesTypeData> retrieveTariffCodeSalesTypeMap(final Iterable<Long> ids) {
        final List<Long> idList = ids == null ? List.of() : java.util.stream.StreamSupport.stream(ids.spliterator(), false)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (idList.isEmpty()) {
            return Map.of();
        }
        final String placeholders = idList.stream().map(id -> "?").collect(Collectors.joining(","));
        final Object[] args = idList.toArray();
        return jdbcTemplate.query("""
                SELECT id, code, description
                FROM reference.ref_tariff_code_sales_type
                WHERE is_deleted = FALSE AND id IN (%s)
                """.formatted(placeholders), (rs, rowNum) -> TariffCodeSalesTypeData.builder()
                .id(rs.getLong("id"))
                .code(rs.getString("code"))
                .description(rs.getString("description"))
                .build(), args).stream()
                .collect(Collectors.toMap(TariffCodeSalesTypeData::getId, row -> row));
    }
}
