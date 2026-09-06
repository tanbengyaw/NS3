package my.gov.perkeso.assist.registration.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import my.gov.perkeso.assist.registration.constant.TaxType;
import my.gov.perkeso.assist.registration.data.TaxPayerUpdateSearchResultData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Search existing active taxpayers for the Update Tax Payer workflow (GET
 * /v1/reference/tax-payer-updates). Joins {@code registration.employer} + {@code business_info} +
 * {@code sst_info} on employer id, filtered by the tax-type-specific SMK column being present
 * (i.e. the employer is actually active for that tax type) and matching the search term against
 * employer name, BRN, or the SMK reg no itself.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxPayerUpdateSearchReadPlatformService {

    private final JdbcTemplate jdbcTemplate;

    public List<TaxPayerUpdateSearchResultData> search(final String taxTypeParam, final String search) {
        final TaxType taxType = parseTaxType(taxTypeParam);
        final String smkColumn = smkColumnFor(taxType);
        final long sectionId = updateSectionIdFor(taxType);

        final String term = search == null ? "" : search.trim();
        final String pattern = "%" + term + "%";

        final String sql = """
                SELECT e.id AS employer_id, e.employer_name AS employer_name, bi.registration_no AS registration_no,
                       si.%1$s AS smk_reg_no
                FROM registration.sst_info si
                JOIN registration.employer e ON e.id = si.employer_id AND e.is_deleted = false
                JOIN registration.business_info bi ON bi.id = e.business_info_id
                WHERE si.is_deleted = false
                  AND si.%1$s IS NOT NULL AND si.%1$s <> ''
                  AND si.id = (SELECT MAX(si2.id) FROM registration.sst_info si2
                               WHERE si2.employer_id = si.employer_id AND si2.is_deleted = false)
                  AND (e.employer_name ILIKE ? OR bi.registration_no ILIKE ? OR si.%1$s ILIKE ?)
                ORDER BY e.employer_name
                LIMIT 50
                """.formatted(smkColumn);

        return jdbcTemplate.query(sql, (rs, rowNum) -> TaxPayerUpdateSearchResultData.builder()
                .employerId(rs.getLong("employer_id"))
                .employerName(rs.getString("employer_name"))
                .registrationNo(rs.getString("registration_no"))
                .smkRegNo(rs.getString("smk_reg_no"))
                .sectionId(sectionId)
                .build(), pattern, pattern, pattern);
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

    private static String smkColumnFor(final TaxType taxType) {
        return switch (taxType) {
            case SERVICE_TAX -> "service_tax_smk_reg_no";
            case SALES_TAX -> "sales_tax_smk_reg_no";
            case TOURISM_TAX -> "tourism_tax_smk_reg_no";
            case DIGITAL_TAX -> "digital_tax_smk_reg_no";
            case DPSP_TAX -> "dpsp_tax_smk_reg_no";
        };
    }

    private static long updateSectionIdFor(final TaxType taxType) {
        return switch (taxType) {
            case SERVICE_TAX -> RegistrationSection.REG_UPDATE_TAX_PAYER_SERVICE_TAX.getAssistSectionId();
            case SALES_TAX -> RegistrationSection.REG_UPDATE_TAX_PAYER_SALES_TAX.getAssistSectionId();
            case TOURISM_TAX -> RegistrationSection.REG_UPDATE_TAX_PAYER_TOURISM_TAX.getAssistSectionId();
            case DIGITAL_TAX -> RegistrationSection.REG_UPDATE_TAX_PAYER_DIGITAL_TAX.getAssistSectionId();
            case DPSP_TAX -> RegistrationSection.REG_UPDATE_TAX_PAYER_DPSP_TAX.getAssistSectionId();
        };
    }
}
