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
@Table(name = "temp_premises", schema = "registration")
@Getter
@Setter
public class TempPremises {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reg_general_info_id", nullable = false)
    private Long regGeneralInfoId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "address_line", nullable = false, length = 200)
    private String addressLine;

    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    @Column(name = "address_line3", length = 200)
    private String addressLine3;

    @Column(name = "post_code", length = 10)
    private String postCode;

    @Column(name = "city_name", length = 100)
    private String cityName;

    @Column(name = "state_name", length = 100)
    private String stateName;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}
