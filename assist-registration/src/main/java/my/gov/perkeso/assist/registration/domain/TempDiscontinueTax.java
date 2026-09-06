package my.gov.perkeso.assist.registration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Draft data for a "Discontinue Tax" case (section 1103) — mirrors ASSIST DiscontinueTaxFormDto.
 * One row per {@code RegGeneralInfo}, targeting a single (sstInfoId, taxTypeId) pair.
 */
@Entity
@Table(name = "temp_discontinue_tax", schema = "registration")
@Getter
@Setter
public class TempDiscontinueTax {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reg_general_info_id", nullable = false, unique = true)
    private Long regGeneralInfoId;

    @Column(name = "sst_info_id", nullable = false)
    private Long sstInfoId;

    @Column(name = "tax_type_id", nullable = false)
    private Long taxTypeId;

    @Column(name = "new_sst_status_id")
    private Long newSstStatusId;

    @Column(name = "cessation_tax_effective_from")
    private LocalDate cessationTaxEffectiveFrom;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
}
