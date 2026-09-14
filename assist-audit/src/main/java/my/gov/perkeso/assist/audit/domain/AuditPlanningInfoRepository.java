package my.gov.perkeso.assist.audit.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditPlanningInfoRepository extends JpaRepository<AuditPlanningInfo, Long> {

    Optional<AuditPlanningInfo> findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(Long auditCaseId);
}
