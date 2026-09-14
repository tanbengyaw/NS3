package my.gov.perkeso.assist.audit.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxpaperResponseRepository extends JpaRepository<TaxpaperResponse, Long> {

    Optional<TaxpaperResponse> findFirstByAuditCaseIdAndActivatedTrueAndDeletedFalseOrderByIdDesc(Long auditCaseId);

    List<TaxpaperResponse> findByAuditCaseIdAndActivatedFalseAndDeletedFalseOrderByIdDesc(Long auditCaseId);
}
