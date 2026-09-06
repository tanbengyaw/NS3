package my.gov.perkeso.assist.registration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ref_sst_service_type", schema = "reference")
@Getter
@Setter
public class RefSstServiceType {

    @Id
    private Long id;

    @Column(name = "code", length = 20, nullable = false)
    private String code;

    @Column(name = "description", length = 500, nullable = false)
    private String description;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "is_accommodation", nullable = false)
    private boolean accommodation;
}
