package my.gov.perkeso.assist.registration.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.constant.SstStatus;
import my.gov.perkeso.assist.registration.constant.TaxType;
import my.gov.perkeso.assist.registration.data.DiscontinueTaxSearchResultData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Search currently-ACTIVE taxpayers for the Discontinue Tax workflow (GET
 * /v1/reference/discontinue-tax-payers). Unlike the Update Tax Payer search (which just checks a
 * populated SMK column), this joins the CURRENT {@code sst_status_info} row for the requested tax
 * type so that already-discontinued (CANCEL) registrations don't show up again.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiscontinueTaxSearchReadPlatformService {

    private final JdbcTemplate jdbcTemplate;

    public List<DiscontinueTaxSearchResultData> search(final String taxTypeParam, final String search) {
        final TaxType taxType = parseTaxType(taxTypeParam);
        final String smkColumn = smkColumnFor(taxType);

        final String term = search == null ? "" : search.trim();
        final String pattern = "%" + term + "%";

        final String sql = """
                SELECT e.id AS employer_id, e.employer_name AS employer_name, bi.registration_no AS registration_no,
                       si.%1$s AS smk_reg_no, si.id AS sst_info_id
                FROM registration.sst_status_info ssi
                JOIN registration.sst_info si ON si.id = ssi.sst_info_id AND si.is_deleted = false
                JOIN registration.employer e ON e.id = si.employer_id AND e.is_deleted = false
                JOIN registration.business_info bi ON bi.id = e.business_info_id
                WHERE ssi.is_deleted = false AND ssi.is_current = true
                  AND ssi.sst_status_id = ? AND ssi.tax_type_id = ?
                  AND (e.employer_name ILIKE ? OR bi.registration_no ILIKE ? OR si.%1$s ILIKE ?)
                ORDER BY e.employer_name
                LIMIT 50
                """.formatted(smkColumn);

        return jdbcTemplate.query(sql, (rs, rowNum) -> DiscontinueTaxSearchResultData.builder()
                .employerId(rs.getLong("employer_id"))
                .employerName(rs.getString("employer_name"))
                .registrationNo(rs.getString("registration_no"))
                .smkRegNo(rs.getString("smk_reg_no"))
                .sstInfoId(rs.getLong("sst_info_id"))
                .taxTypeId(taxType.getAssistId())
                .taxTypeLabel(taxType.name())
                .build(), SstStatus.ACTIVE.getAssistId(), taxType.getAssistId(), pattern, pattern, pattern);
    }

    private static TaxType parseTaxType(final String taxTypeParam) {
        if (taxTypeParam == null || taxTypeParam.isBlank()) {
            throw new IllegalArgumentException("taxType is required");
        }
        try {
            return TaxType.valueOf(taxTypeParam.trim().toUpperCase());
        } catch (final IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported taxType: " + taxTypeParam
                    + " (expected one of SERVICE_TAX, SALES_TAX, TOURISM_TAX, DIGITAL_TAX, DPSP_TAX)", ex);
        }
    }

    static String smkColumnFor(final TaxType taxType) {
        return switch (taxType) {
            case SERVICE_TAX -> "service_tax_smk_reg_no";
            case SALES_TAX -> "sales_tax_smk_reg_no";
            case TOURISM_TAX -> "tourism_tax_smk_reg_no";
            case DIGITAL_TAX -> "digital_tax_smk_reg_no";
            case DPSP_TAX -> "dpsp_tax_smk_reg_no";
        };
    }
}
