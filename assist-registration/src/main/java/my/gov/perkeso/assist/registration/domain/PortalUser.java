package my.gov.perkeso.assist.registration.domain;

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
@Table(name = "portal_user", schema = "registration")
@Getter
@Setter
public class PortalUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "employer_id")
    private Long employerId;

    @Column(name = "employer_code", length = 30)
    private String employerCode;

    @Column(name = "employer_name", length = 200)
    private String employerName;

    @Column(name = "registration_type_id")
    private Long registrationTypeId;

    @Column(name = "registration_no", length = 50)
    private String registrationNo;

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

    @Column(name = "post_code", length = 10)
    private String postCode;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(name = "identification_type_id")
    private Long identificationTypeId;

    @Column(name = "identification_no", length = 50)
    private String identificationNo;

    @Column(name = "phone_calling_code", length = 10)
    private String phoneCallingCode;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "security_phrase", length = 100)
    private String securityPhrase;

    @Column(name = "enrollment_status", length = 30)
    private String enrollmentStatus;

    @Column(name = "query_remark", length = 500)
    private String queryRemark;

    @Column(name = "linked_date")
    private LocalDateTime linkedDate;

    @Column(name = "application_type", length = 30)
    private String applicationType;

    @Column(name = "enrolled_date")
    private LocalDateTime enrolledDate;

    @Column(name = "user_employer_id")
    private Long userEmployerId;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "active", nullable = false)
    private boolean active = false;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    public boolean isLoginEnabled() {
        return active && passwordHash != null && !passwordHash.isBlank();
    }
}
