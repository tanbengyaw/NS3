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

    @Column(name = "linked_date")
    private LocalDateTime linkedDate;

    @Column(name = "application_type", length = 30)
    private String applicationType;

    @Column(name = "enrolled_date")
    private LocalDateTime enrolledDate;
}
