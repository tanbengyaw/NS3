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
@Table(name = "temp_director_owner", schema = "registration")
@Getter
@Setter
public class TempDirectorOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reg_general_info_id", nullable = false)
    private Long regGeneralInfoId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "identification_type_id")
    private Long identificationTypeId;

    @Column(name = "identification_no", nullable = false, length = 30)
    private String identificationNo;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}
