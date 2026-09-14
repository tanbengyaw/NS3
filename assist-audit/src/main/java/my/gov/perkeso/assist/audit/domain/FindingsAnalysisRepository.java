package my.gov.perkeso.assist.audit.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FindingsAnalysisRepository extends JpaRepository<FindingsAnalysis, Long> {

    Optional<FindingsAnalysis> findFirstByAuditCaseIdAndDeletedFalseOrderByIdAsc(Long auditCaseId);
}
