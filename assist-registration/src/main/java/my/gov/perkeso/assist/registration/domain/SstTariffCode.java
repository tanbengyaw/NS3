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
@Table(name = "sst_tariff_code", schema = "registration")
@Getter
@Setter
public class SstTariffCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sst_info_id", nullable = false)
    private Long sstInfoId;

    @Column(name = "employer_id", nullable = false)
    private Long employerId;

    @Column(name = "tariff_code_sales_type_id", nullable = false)
    private Long tariffCodeSalesTypeId;

    @Column(name = "contract_type_id", nullable = false)
    private Long contractTypeId;

    @Column(name = "finished_goods", length = 200)
    private String finishedGoods;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}
