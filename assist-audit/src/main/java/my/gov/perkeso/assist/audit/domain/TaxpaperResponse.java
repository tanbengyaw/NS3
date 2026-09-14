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
@Table(name = "taxpaper_response", schema = "audit")
@Getter
@Setter
public class TaxpaperResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_case_id", nullable = false)
    private Long auditCaseId;

    @Column(name = "response_id", nullable = false, length = 30)
    private String responseId;

    @Column(name = "response_date")
    private LocalDate responseDate;

    @Column(name = "response_channel")
    private Integer responseChannel;

    @Column(name = "response_type")
    private Integer responseType;

    @Column(name = "employer_id")
    private Long employerId;

    @Column(name = "taxpaper_comments", length = 1000)
    private String taxpayerComments;

    @Column(name = "officers_status")
    private Integer officersStatus;

    @Column(name = "officers_final_outcome")
    private Integer officersFinalOutcome;

    @Column(name = "officers_remarks", length = 1000)
    private String officersRemarks;

    @Column(name = "is_activated", nullable = false)
    private boolean activated;

    @Column(name = "revision_amount", precision = 16, scale = 2)
    private BigDecimal revisionAmount;

    @Column(name = "revision_reason", length = 1000)
    private String revisionReason;

    @Column(name = "cancellation_id", length = 30)
    private String cancellationId;

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
