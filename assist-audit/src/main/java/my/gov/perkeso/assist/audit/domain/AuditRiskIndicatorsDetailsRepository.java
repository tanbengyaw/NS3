package my.gov.perkeso.assist.audit.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRiskIndicatorsDetailsRepository extends JpaRepository<AuditRiskIndicatorsDetails, Long> {

    Optional<AuditRiskIndicatorsDetails> findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(Long auditCaseId);
}
