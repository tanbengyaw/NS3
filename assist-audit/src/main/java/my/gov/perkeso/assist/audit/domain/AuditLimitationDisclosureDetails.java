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
@Table(name = "audit_limitation_disclosure_details", schema = "audit")
@Getter
@Setter
public class AuditLimitationDisclosureDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_case_id", nullable = false)
    private Long auditCaseId;

    @Column(name = "is_checked_data_incomplete", nullable = false)
    private boolean checkedDataIncomplete;

    @Column(name = "is_checked_access_limitation", nullable = false)
    private boolean checkedAccessLimitation;

    @Column(name = "is_checked_preliminary_assumptions", nullable = false)
    private boolean checkedPreliminaryAssumptions;

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
