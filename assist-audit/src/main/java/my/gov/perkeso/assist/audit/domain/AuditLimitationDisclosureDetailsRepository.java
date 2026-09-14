package my.gov.perkeso.assist.audit.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLimitationDisclosureDetailsRepository
        extends JpaRepository<AuditLimitationDisclosureDetails, Long> {

    Optional<AuditLimitationDisclosureDetails> findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(Long auditCaseId);
}
