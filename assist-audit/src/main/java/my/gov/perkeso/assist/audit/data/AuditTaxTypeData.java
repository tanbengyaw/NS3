package my.gov.perkeso.assist.audit.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuditTaxTypeData {

    Long id;
    String taxType;
    Long refTaxRegTypeId;
    boolean selectedAudit;
    LocalDate businessComDate;
    LocalDate manSerComDate;
    Long finYrEndMon;
    BigDecimal annualTtlTaxSalSerVal;
    Long sstInfoId;
}
