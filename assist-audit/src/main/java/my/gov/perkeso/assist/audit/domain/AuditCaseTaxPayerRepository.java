package my.gov.perkeso.assist.audit.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditCaseTaxPayerRepository extends JpaRepository<AuditCaseTaxPayer, Long> {

    Optional<AuditCaseTaxPayer> findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(Long auditCaseId);
}
