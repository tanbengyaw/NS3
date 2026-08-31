package my.gov.perkeso.assist.registration.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "employer", schema = "registration")
@Getter
@Setter
public class Employer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employer_code", nullable = false, unique = true, length = 30)
    private String employerCode;

    @Column(name = "employer_name", nullable = false, length = 100)
    private String employerName;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "business_info_id", nullable = false)
    private BusinessInfo businessInfo;

    @Column(name = "service_type_id", nullable = false)
    private Long serviceTypeId;

    @Column(name = "pks_branch_id", nullable = false)
    private Long pksBranchId;

    @Column(name = "is_branch", nullable = false)
    private boolean branch;

    @Column(name = "msic_id")
    private Long msicId;

    @Column(name = "method_contribution_payment_id")
    private Long methodContributionPaymentId;

    @Column(name = "is_contribution_active")
    private boolean contributionActive;

    @Column(name = "employer_register_status")
    private Integer employerRegisterStatus;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "created_by_id")
    private Long createdById;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_by_id")
    private Long updatedById;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;
}
