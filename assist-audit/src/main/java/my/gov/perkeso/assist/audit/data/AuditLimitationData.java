package my.gov.perkeso.assist.audit.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuditLimitationData {

    boolean checkedDataIncomplete;
    boolean checkedAccessLimitation;
    boolean checkedPreliminaryAssumptions;
    boolean checkedOthers;
    String othersInput;
}
