package my.gov.perkeso.assist.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "working_paper", schema = "audit")
@Getter
@Setter
public class WorkingPaper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_case_id", nullable = false)
    private Long auditCaseId;

    @Column(name = "case_ref_no", length = 30)
    private String caseRefNo;

    @Column(name = "ref_working_paper_status_id")
    private Long refWorkingPaperStatusId;

    @Column(name = "finding_usage", nullable = false)
    private boolean findingUsage;

    @Column(name = "ref_focus_area_id")
    private Long refFocusAreaId;

    @Column(name = "procedures_detail", length = 1000)
    private String proceduresDetail;

    @Column(name = "ref_test_performed_id")
    private Long refTestPerformedId;

    @Column(name = "test_performed_other", length = 1000)
    private String testPerformedOther;

    @Column(name = "test_description", length = 1000)
    private String testDescription;

    @Column(name = "population_size")
    private Integer populationSize;

    @Column(name = "population_total_val", precision = 16, scale = 2)
    private BigDecimal populationTotalVal;

    @Column(name = "population_period_from")
    private LocalDateTime populationPeriodFrom;

    @Column(name = "population_period_to")
    private LocalDateTime populationPeriodTo;

    @Column(name = "population_description", length = 1000)
    private String populationDescription;

    @Column(name = "ref_sampling_method_id")
    private Long refSamplingMethodId;

    @Column(name = "sampling_method_other", length = 1000)
    private String samplingMethodOther;

    @Column(name = "sample_count")
    private Integer sampleCount;

    @Column(name = "sample_value", precision = 16, scale = 2)
    private BigDecimal sampleValue;

    @Column(name = "sample_period_from")
    private LocalDateTime samplePeriodFrom;

    @Column(name = "sample_period_to")
    private LocalDateTime samplePeriodTo;

    @Column(name = "sampling_detail", length = 1000)
    private String samplingDetail;

    @Column(name = "declared_amount", precision = 16, scale = 2)
    private BigDecimal declaredAmount;

    @Column(name = "correct_amount", precision = 16, scale = 2)
    private BigDecimal correctAmount;

    @Column(name = "total_error_in_sample", precision = 16, scale = 2)
    private BigDecimal totalErrorInSample;

    @Column(name = "error_rate", precision = 16, scale = 2)
    private BigDecimal errorRate;

    @Column(name = "error_sample_comp_detail", length = 1000)
    private String errorSampleCompDetail;

    @Column(name = "ref_projection_method_id")
    private Long refProjectionMethodId;

    @Column(name = "projection_justification", length = 1000)
    private String projectionJustification;

    @Column(name = "business_stability_confirmed")
    private Boolean businessStabilityConfirmed;

    @Column(name = "structural_charge_detected")
    private Boolean structuralChargeDetected;

    @Column(name = "ref_tax_type_id")
    private Long refTaxTypeId;

    @Column(name = "projected_adjustment", precision = 16, scale = 2)
    private BigDecimal projectedAdjustment;

    @Column(name = "applicable_tax_rate", precision = 5, scale = 2)
    private BigDecimal applicableTaxRate;

    @Column(name = "additional_tax", precision = 16, scale = 2)
    private BigDecimal additionalTax;

    @Column(name = "projected_tax_impact", precision = 16, scale = 2)
    private BigDecimal projectedTaxImpact;

    @Column(name = "ref_finding_type_id")
    private Long refFindingTypeId;

    @Column(name = "finding_type_other", length = 1000)
    private String findingTypeOther;

    @Column(name = "conclution", length = 1000)
    private String conclution;

    @Column(name = "assumptions", length = 1000)
    private String assumptions;

    @Column(name = "limitations", length = 1000)
    private String limitations;

    @Column(name = "finding_analysis_ref_no", length = 30)
    private String findingAnalysisRefNo;

    @Column(name = "finding_description", length = 1000)
    private String findingDescription;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "create_by_id")
    private Long createById;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    @Column(name = "update_by_id")
    private Long updateById;

    @Column(name = "update_date")
    private LocalDateTime updateDate;
}
