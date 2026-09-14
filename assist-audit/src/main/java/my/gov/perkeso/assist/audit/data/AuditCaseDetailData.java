package my.gov.perkeso.assist.audit.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuditCaseDetailData {

    Long id;
    String caseRefNo;
    Long taskStatusId;
    String taskStatusLabel;
    boolean submitted;
    boolean preAuditSkip;
    Long refCaseSourceId;
    Long refRiskLevelId;
    BigDecimal extimatedTaxExposureRm;
    LocalDate periodFrom;
    LocalDate periodTo;
    String objective;
    String exclusions;
    String justification;
    String cusAudRefNo;
    Long refProposedCaseTypeId;
    AuditTaxpayerData taxpayer;
    List<AuditTaxTypeData> taxTypes;
    AuditRiskData risk;
    AuditFocusData focus;
    AuditLimitationData limitation;
    AuditPlanningData planning;
    AuditFieldWorkData fieldWork;
    List<AuditWorkingPaperData> workingPapers;
    AuditFindingsData findings;
    AuditTaxpayerResponseData taxpayerResponse;
    List<AuditTaxpayerResponseData> previousTaxpayerResponses;
}
