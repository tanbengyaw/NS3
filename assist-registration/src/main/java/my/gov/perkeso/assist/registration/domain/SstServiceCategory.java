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
@Table(name = "sst_service_category", schema = "registration")
@Getter
@Setter
public class SstServiceCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sst_info_id", nullable = false)
    private Long sstInfoId;

    @Column(name = "employer_id", nullable = false)
    private Long employerId;

    @Column(name = "sst_service_type_id", nullable = false)
    private Long sstServiceTypeId;

    @Column(name = "remark", length = 255)
    private String remark;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}
