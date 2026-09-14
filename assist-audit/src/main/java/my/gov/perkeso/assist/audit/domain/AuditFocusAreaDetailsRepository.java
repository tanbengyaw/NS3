package my.gov.perkeso.assist.audit.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditFocusAreaDetailsRepository extends JpaRepository<AuditFocusAreaDetails, Long> {

    Optional<AuditFocusAreaDetails> findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(Long auditCaseId);
}
