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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "temp_employer", schema = "registration")
@Getter
@Setter
public class TempEmployer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "employer_register_status")
    private Integer employerRegisterStatus;

    @Column(name = "post_code", length = 10)
    private String postCode;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "contact_phones", columnDefinition = "TEXT")
    private String contactPhones;

    @Column(name = "contact_faxes", columnDefinition = "TEXT")
    private String contactFaxes;

    @Column(name = "address_line1", length = 200)
    private String addressLine1;

    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    @Column(name = "address_line3", length = 200)
    private String addressLine3;

    @Column(name = "state_id")
    private Long stateId;

    @Column(name = "city_id")
    private Long cityId;

    @Column(name = "city_name", length = 100)
    private String cityName;

    @Column(name = "corr_address_line1", length = 200)
    private String corrAddressLine1;

    @Column(name = "corr_address_line2", length = 200)
    private String corrAddressLine2;

    @Column(name = "corr_address_line3", length = 200)
    private String corrAddressLine3;

    @Column(name = "corr_post_code", length = 10)
    private String corrPostCode;

    @Column(name = "corr_state_id")
    private Long corrStateId;

    @Column(name = "corr_city_id")
    private Long corrCityId;

    @Column(name = "corr_city_name", length = 100)
    private String corrCityName;
}
