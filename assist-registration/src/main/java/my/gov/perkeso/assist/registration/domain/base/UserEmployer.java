package my.gov.perkeso.assist.registration.domain.base;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_employer", schema = "base")
@Getter
@Setter
public class UserEmployer extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "application_type", nullable = false)
    private int applicationType;

    @Column(name = "employer_code", length = 20)
    private String employerCode;

    @Column(name = "employer_name", length = 100)
    private String employerName;

    @Column(name = "registration_type_id")
    private Long registrationTypeId;

    @Column(name = "registration_no", length = 30)
    private String registrationNo;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "identification_type", nullable = false)
    private Long identificationType;

    @Column(name = "identification_value", nullable = false, length = 30)
    private String identificationValue;

    @Column(name = "group_of_companies", nullable = false)
    private boolean groupOfCompanies;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "employer_id")
    private Long employerId;
}
