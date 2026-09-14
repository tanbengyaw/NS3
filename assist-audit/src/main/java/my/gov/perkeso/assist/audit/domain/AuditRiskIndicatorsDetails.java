package my.gov.perkeso.assist.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "audit_risk_indicators_details", schema = "audit")
@Getter
@Setter
public class AuditRiskIndicatorsDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_case_id", nullable = false)
    private Long auditCaseId;

    @Column(name = "is_checked_reg_anomaly", nullable = false)
    private boolean checkedRegAnomaly;

    @Column(name = "is_checked_filing_behaviour", nullable = false)
    private boolean checkedFilingBehaviour;

    @Column(name = "is_checked_payment_behaviour", nullable = false)
    private boolean checkedPaymentBehaviour;

    @Column(name = "is_checked_return_tax", nullable = false)
    private boolean checkedReturnTax;

    @Column(name = "is_checked_financial_anomaly", nullable = false)
    private boolean checkedFinancialAnomaly;

    @Column(name = "is_checked_third_party_mismatch", nullable = false)
    private boolean checkedThirdPartyMismatch;

    @Column(name = "is_checked_his_compliance_issue", nullable = false)
    private boolean checkedHisComplianceIssue;

    @Column(name = "is_checked_intelligence_based", nullable = false)
    private boolean checkedIntelligenceBased;

    @Column(name = "risk_assessment_details", length = 1000)
    private String riskAssessmentDetails;

    @Column(name = "risk_indicators_remark", length = 1000)
    private String riskIndicatorsRemark;

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
