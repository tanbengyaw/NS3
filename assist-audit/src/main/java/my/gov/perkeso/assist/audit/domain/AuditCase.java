package my.gov.perkeso.assist.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "audit_case", schema = "audit")
@Getter
@Setter
public class AuditCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "process_instance_id")
    private Long processInstanceId;

    @Column(name = "case_ref_no", length = 30)
    private String caseRefNo;

    @Column(name = "task_status_id")
    private Long taskStatusId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "submit_date")
    private LocalDate submitDate;

    @Column(name = "is_pre_audit_skip", nullable = false)
    private boolean preAuditSkip;

    @Column(name = "ref_case_source_id")
    private Long refCaseSourceId;

    @Column(name = "ref_risk_level_id")
    private Long refRiskLevelId;

    @Column(name = "ref_audit_reason_id")
    private Long refAuditReasonId;

    @Column(name = "extimated_tax_exposure_rm", precision = 16, scale = 2)
    private BigDecimal extimatedTaxExposureRm;

    @Column(name = "period_from")
    private LocalDate periodFrom;

    @Column(name = "period_to")
    private LocalDate periodTo;

    @Column(name = "objective", length = 1000)
    private String objective;

    @Column(name = "exclusions", length = 1000)
    private String exclusions;

    @Column(name = "ref_proposed_next_action_id")
    private Long refProposedNextActionId;

    @Column(name = "ref_proposed_case_type_id")
    private Long refProposedCaseTypeId;

    @Column(name = "review_date")
    private LocalDate reviewDate;

    @Column(name = "justification", length = 1000)
    private String justification;

    @Column(name = "cus_aud_ref_no", length = 50)
    private String cusAudRefNo;

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
