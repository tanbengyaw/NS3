package my.gov.perkeso.assist.registration.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.constant.RegistrationSectionRouting;
import my.gov.perkeso.assist.registration.constant.TaxType;
import my.gov.perkeso.assist.registration.data.IncompleteAutoRegSearchResultData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Staff listing of audit auto-reg SST rows that are still incomplete (legacy
 * {@code searchIncompleteAutoRegListing}).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncompleteAutoRegSearchReadPlatformService {

    private final JdbcTemplate jdbcTemplate;

    public List<IncompleteAutoRegSearchResultData> search(final String taxTypeParam, final String search) {
        final TaxType taxType = IncompleteAutoRegCompleteness.parseTaxType(taxTypeParam);
        final long sectionId = RegistrationSectionRouting.incompleteSectionIdFor(taxType);
        final String term = search == null ? "" : search.trim();
        final String pattern = "%" + term + "%";
        final boolean salesOrService = taxType == TaxType.SALES_TAX || taxType == TaxType.SERVICE_TAX;

        final String completeness = salesOrService
                ? "(si.man_com_date IS NULL OR si.date_sale_val_tax_goods IS NULL)"
                : "(si.applicant_name IS NULL OR TRIM(si.applicant_name) = '')";

        final String sql = """
                SELECT si.id AS sst_info_id, e.id AS employer_id, e.employer_name AS employer_name,
                       bi.registration_no AS registration_no, si.tax_type AS tax_type,
                       si.cus_aud_ref_no AS cus_aud_ref_no, si.business_com_date AS business_com_date,
                       si.created_date AS created_date,
                       (SELECT g.id FROM registration.reg_general_info g
                        WHERE g.source_sst_info_id = si.id
                          AND g.app_status IN ('NEW', 'IN_PROGRESS', 'IN_QUERY', 'SUBMITTED')
                        ORDER BY g.id DESC LIMIT 1) AS open_case_id,
                       (SELECT g.case_ref_no FROM registration.reg_general_info g
                        WHERE g.source_sst_info_id = si.id
                          AND g.app_status IN ('NEW', 'IN_PROGRESS', 'IN_QUERY', 'SUBMITTED')
                        ORDER BY g.id DESC LIMIT 1) AS open_case_ref_no
                FROM registration.sst_info si
                JOIN registration.employer e ON e.id = si.employer_id AND e.is_deleted = false
                JOIN registration.business_info bi ON bi.id = e.business_info_id
                WHERE si.is_deleted = false
                  AND si.is_auto_registration = true
                  AND si.tax_type = ?
                  AND %1$s
                  AND (e.employer_name ILIKE ? OR bi.registration_no ILIKE ?
                       OR COALESCE(si.cus_aud_ref_no, '') ILIKE ?)
                ORDER BY si.created_date DESC, e.employer_name
                LIMIT 50
                """.formatted(completeness);

        return jdbcTemplate.query(sql, (rs, rowNum) -> IncompleteAutoRegSearchResultData.builder()
                .sstInfoId(rs.getLong("sst_info_id"))
                .employerId(rs.getLong("employer_id"))
                .employerName(rs.getString("employer_name"))
                .registrationNo(rs.getString("registration_no"))
                .taxType(rs.getString("tax_type"))
                .sectionId(sectionId)
                .cusAudRefNo(rs.getString("cus_aud_ref_no"))
                .businessComDate(rs.getDate("business_com_date") != null
                        ? rs.getDate("business_com_date").toLocalDate() : null)
                .createdDate(rs.getTimestamp("created_date") != null
                        ? rs.getTimestamp("created_date").toLocalDateTime() : null)
                .openCaseId(rs.getObject("open_case_id") != null ? rs.getLong("open_case_id") : null)
                .openCaseRefNo(rs.getString("open_case_ref_no"))
                .build(), taxType.name(), pattern, pattern, pattern);
    }
}
