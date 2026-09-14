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
@Table(name = "audit_case_tax_payer", schema = "audit")
@Getter
@Setter
public class AuditCaseTaxPayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_case_id", nullable = false)
    private Long auditCaseId;

    @Column(name = "employer_id")
    private Long employerId;

    @Column(name = "tax_payer_name", length = 100)
    private String taxPayerName;

    @Column(name = "business_reg_no", length = 30)
    private String businessRegNo;

    @Column(name = "address_line_1", length = 150)
    private String addressLine1;

    @Column(name = "address_line_2", length = 150)
    private String addressLine2;

    @Column(name = "address_line_3", length = 150)
    private String addressLine3;

    @Column(name = "ref_state_id")
    private Long refStateId;

    @Column(name = "ref_city_id")
    private Long refCityId;

    @Column(name = "postcode", length = 10)
    private String postcode;

    @Column(name = "ns3_branch_id")
    private Long ns3BranchId;

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
