package my.gov.perkeso.assist.registration.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.constant.DataSource;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Routes portal registration cases to a processing branch from business postcode
 * ({@code reference.ref_postcode_office}), mirroring legacy ASSIST counter routing.
 */
@Service
@RequiredArgsConstructor
public class PostcodeBranchRoutingService {

    private final JdbcTemplate jdbcTemplate;

    public Optional<Long> resolvePrimaryBranchIdForPostcode(final String postcode) {
        if (postcode == null || postcode.isBlank()) {
            return Optional.empty();
        }
        final List<Long> branchIds = jdbcTemplate.query("""
                SELECT po.branch_id
                FROM reference.ref_postcode_office po
                WHERE po.postcode = ?
                ORDER BY po.sort_order DESC, po.branch_id
                LIMIT 1
                """, (rs, rowNum) -> rs.getLong("branch_id"), postcode.trim());
        return branchIds.isEmpty() ? Optional.empty() : Optional.of(branchIds.get(0));
    }

    public void applyPortalSubmitBranchRouting(final RegGeneralInfo regCase) {
        if (regCase.getDataSourceId() != DataSource.PORTAL.getAssistId()) {
            return;
        }
        final String postcode = regCase.getTempEmployer() != null ? regCase.getTempEmployer().getPostCode() : null;
        resolvePrimaryBranchIdForPostcode(postcode).ifPresent(branchId -> {
            regCase.setProcessingPksBranchId(branchId);
            regCase.setReceivingPksBranchId(branchId);
        });
    }
}
