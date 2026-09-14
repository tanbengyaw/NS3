package my.gov.perkeso.assist.audit.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuditRiskData {

    boolean checkedRegAnomaly;
    boolean checkedFilingBehaviour;
    boolean checkedPaymentBehaviour;
    boolean checkedReturnTax;
    boolean checkedFinancialAnomaly;
    boolean checkedThirdPartyMismatch;
    boolean checkedHisComplianceIssue;
    boolean checkedIntelligenceBased;
    String riskAssessmentDetails;
    String riskIndicatorsRemark;
}
