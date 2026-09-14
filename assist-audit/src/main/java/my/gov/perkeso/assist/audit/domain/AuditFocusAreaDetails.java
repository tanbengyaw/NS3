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
@Table(name = "audit_focus_area_details", schema = "audit")
@Getter
@Setter
public class AuditFocusAreaDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_case_id", nullable = false)
    private Long auditCaseId;

    @Column(name = "is_checked_output_tax", nullable = false)
    private boolean checkedOutputTax;

    @Column(name = "is_checked_input_tax_eligility", nullable = false)
    private boolean checkedInputTaxEligility;

    @Column(name = "is_checked_refund", nullable = false)
    private boolean checkedRefund;

    @Column(name = "is_checked_classification", nullable = false)
    private boolean checkedClassification;

    @Column(name = "is_checked_exemption", nullable = false)
    private boolean checkedExemption;

    @Column(name = "is_checked_import_reconciliation", nullable = false)
    private boolean checkedImportReconciliation;

    @Column(name = "is_checked_revenue_reconciliation", nullable = false)
    private boolean checkedRevenueReconciliation;

    @Column(name = "is_checked_others", nullable = false)
    private boolean checkedOthers;

    @Column(name = "others_input", length = 100)
    private String othersInput;

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
