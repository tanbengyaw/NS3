package my.gov.perkeso.assist.audit.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxpayerTaxTypeInfoRepository extends JpaRepository<TaxpayerTaxTypeInfo, Long> {

    List<TaxpayerTaxTypeInfo> findByAuditCaseTaxPayerIdAndDeletedFalseOrderByIdAsc(Long auditCaseTaxPayerId);
}
