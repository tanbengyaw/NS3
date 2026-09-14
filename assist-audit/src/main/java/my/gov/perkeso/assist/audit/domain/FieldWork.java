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
@Table(name = "field_work", schema = "audit")
@Getter
@Setter
public class FieldWork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_case_id", nullable = false)
    private Long auditCaseId;

    @Column(name = "ref_visit_type_id")
    private Long refVisitTypeId;

    @Column(name = "other", length = 1000)
    private String other;

    @Column(name = "observation", length = 1000)
    private String observation;

    @Column(name = "ref_site_visit_outcome_id")
    private Long refSiteVisitOutcomeId;

    @Column(name = "officer_remark", length = 1000)
    private String officerRemark;

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
