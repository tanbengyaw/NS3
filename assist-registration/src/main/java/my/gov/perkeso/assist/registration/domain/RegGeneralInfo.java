package my.gov.perkeso.assist.registration.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "reg_general_info", schema = "registration")
@Getter
@Setter
public class RegGeneralInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_ref_no", nullable = false, unique = true, length = 30)
    private String caseRefNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_status", nullable = false, length = 20)
    private AppStatus appStatus;

    @Column(name = "app_status_reason", length = 500)
    private String appStatusReason;

    @Column(name = "reg_type", length = 50)
    private String regType;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(name = "data_source_id", nullable = false)
    private Long dataSourceId;

    @Column(name = "pks_branch_id", nullable = false)
    private Long pksBranchId;

    @Column(name = "processing_pks_branch_id", nullable = false)
    private Long processingPksBranchId;

    @Column(name = "receiving_pks_branch_id", nullable = false)
    private Long receivingPksBranchId;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "temp_employer_id", nullable = false)
    private TempEmployer tempEmployer;

    @Column(name = "employer_id")
    private Long employerId;

    @Column(name = "created_by_id")
    private Long createdById;

    @Column(name = "created_by_username", length = 100)
    private String createdByUsername;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "document_received_date")
    private LocalDateTime documentReceivedDate;

    @Column(name = "submission_date")
    private LocalDateTime submissionDate;

    @Column(name = "submitted_by_username", length = 100)
    private String submittedByUsername;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "is_incomplete", nullable = false)
    private boolean incomplete;

    @Column(name = "inquery_by_username", length = 100)
    private String inqueryByUsername;

    @Column(name = "inquery_date")
    private LocalDateTime inqueryDate;

    @Column(name = "query_remark", length = 500)
    private String queryRemark;
}
