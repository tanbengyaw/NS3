package my.gov.perkeso.assist.audit.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkingPaperRepository extends JpaRepository<WorkingPaper, Long> {

    List<WorkingPaper> findByAuditCaseIdAndDeletedFalseOrderByIdAsc(Long auditCaseId);
}
