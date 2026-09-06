package my.gov.perkeso.assist.registration.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.data.PortalDocTypeData;
import my.gov.perkeso.assist.registration.data.RefOptionData;
import my.gov.perkeso.assist.registration.data.SupportingDocumentTypeData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BaseReferenceReadPlatformService {

    private final JdbcTemplate jdbcTemplate;

    public List<RefOptionData> retrieveAddressTypes() {
        return jdbcTemplate.query("""
                SELECT id, label
                FROM base.ref_address_type
                WHERE is_deleted = FALSE
                ORDER BY sort_order, label
                """, (rs, rowNum) -> RefOptionData.builder().id(rs.getLong("id")).label(rs.getString("label")).build());
    }

    public List<RefOptionData> retrieveContactTypes() {
        return jdbcTemplate.query("""
                SELECT id, label
                FROM base.ref_contact_type
                WHERE is_deleted = FALSE
                ORDER BY sort_order, label
                """, (rs, rowNum) -> RefOptionData.builder().id(rs.getLong("id")).label(rs.getString("label")).build());
    }

    public List<PortalDocTypeData> retrievePortalDocTypes() {
        return jdbcTemplate.query("""
                SELECT id, code, label, is_required
                FROM base.ref_doc_type
                WHERE is_deleted = FALSE AND module_code = 'PORTAL_ID'
                ORDER BY sort_order, label
                """, (rs, rowNum) -> PortalDocTypeData.builder()
                .id(rs.getLong("id"))
                .code(rs.getString("code"))
                .label(rs.getString("label"))
                .requiredForPortalId(rs.getBoolean("is_required"))
                .build());
    }

    public PortalDocTypeData requirePortalDocType(final Long documentTypeId) {
        return retrievePortalDocTypes().stream()
                .filter(type -> type.getId().equals(documentTypeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown portal document type: " + documentTypeId));
    }

    public List<SupportingDocumentTypeData> retrieveSupportingDocumentTypesForSalesTax() {
        return jdbcTemplate.query("""
                SELECT dt.id, dt.code, dt.label, reg.is_required
                FROM base.ref_doc_section_reg reg
                JOIN base.ref_doc_type dt ON dt.id = reg.doc_type_id
                WHERE reg.is_deleted = FALSE
                  AND dt.is_deleted = FALSE
                ORDER BY reg.sort_order, dt.label
                """, (rs, rowNum) -> SupportingDocumentTypeData.builder()
                .id(rs.getLong("id"))
                .code(rs.getString("code"))
                .label(rs.getString("label"))
                .requiredForSalesTax(rs.getBoolean("is_required"))
                .build());
    }

    public SupportingDocumentTypeData requireSalesTaxDocumentType(final Long documentTypeId) {
        return retrieveSupportingDocumentTypesForSalesTax().stream()
                .filter(type -> type.getId().equals(documentTypeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown supporting document type: " + documentTypeId));
    }

    public List<SupportingDocumentTypeData> retrieveRequiredSupportingDocumentTypesForSalesTax() {
        return retrieveSupportingDocumentTypesForSalesTax().stream()
                .filter(SupportingDocumentTypeData::isRequiredForSalesTax)
                .toList();
    }
}
