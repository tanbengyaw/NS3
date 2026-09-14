package my.gov.perkeso.assist.audit.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditCaseRepository extends JpaRepository<AuditCase, Long> {

    @Query("""
            select c from AuditCase c
            where c.deleted = false
              and (:search is null or :search = ''
                or lower(c.caseRefNo) like lower(concat('%', :search, '%'))
                or exists (
                    select 1 from AuditCaseTaxPayer t
                    where t.auditCaseId = c.id and t.deleted = false
                      and (lower(coalesce(t.taxPayerName, '')) like lower(concat('%', :search, '%'))
                        or lower(coalesce(t.businessRegNo, '')) like lower(concat('%', :search, '%'))))
              )
            order by c.id desc
            """)
    List<AuditCase> search(@Param("search") String search);
}
