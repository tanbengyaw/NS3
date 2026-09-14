package my.gov.perkeso.assist.audit.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuditFocusData {

    boolean checkedOutputTax;
    boolean checkedInputTaxEligility;
    boolean checkedRefund;
    boolean checkedClassification;
    boolean checkedExemption;
    boolean checkedImportReconciliation;
    boolean checkedRevenueReconciliation;
    boolean checkedOthers;
    String othersInput;
}
