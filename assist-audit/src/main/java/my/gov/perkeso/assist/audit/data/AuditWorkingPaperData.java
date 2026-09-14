package my.gov.perkeso.assist.audit.data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuditWorkingPaperData {

    Long id;
    String caseRefNo;
    Long refWorkingPaperStatusId;
    boolean findingUsage;
    Long refFocusAreaId;
    String proceduresDetail;
    Long refTestPerformedId;
    String testPerformedOther;
    String testDescription;
    Integer populationSize;
    BigDecimal populationTotalVal;
    LocalDateTime populationPeriodFrom;
    LocalDateTime populationPeriodTo;
    String populationDescription;
    Long refSamplingMethodId;
    String samplingMethodOther;
    Integer sampleCount;
    BigDecimal sampleValue;
    LocalDateTime samplePeriodFrom;
    LocalDateTime samplePeriodTo;
    String samplingDetail;
    BigDecimal declaredAmount;
    BigDecimal correctAmount;
    BigDecimal totalErrorInSample;
    BigDecimal errorRate;
    String errorSampleCompDetail;
    Long refProjectionMethodId;
    String projectionJustification;
    Boolean businessStabilityConfirmed;
    Boolean structuralChargeDetected;
    Long refTaxTypeId;
    BigDecimal projectedAdjustment;
    BigDecimal applicableTaxRate;
    BigDecimal additionalTax;
    BigDecimal projectedTaxImpact;
    Long refFindingTypeId;
    String findingTypeOther;
    String conclution;
    String assumptions;
    String limitations;
    String findingAnalysisRefNo;
    String findingDescription;
}
