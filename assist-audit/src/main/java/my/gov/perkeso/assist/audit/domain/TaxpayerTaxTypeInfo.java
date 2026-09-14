package my.gov.perkeso.assist.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tax_payer_tax_type_info", schema = "audit")
@Getter
@Setter
public class TaxpayerTaxTypeInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_case_tax_payer_id", nullable = false)
    private Long auditCaseTaxPayerId;

    @Column(name = "sst_info_id")
    private Long sstInfoId;

    @Column(name = "ref_tax_reg_type_id")
    private Long refTaxRegTypeId;

    @Column(name = "business_com_date")
    private LocalDate businessComDate;

    @Column(name = "man_ser_com_date")
    private LocalDate manSerComDate;

    @Column(name = "ref_fin_yr_end_month_id")
    private Long refFinYrEndMonthId;

    @Column(name = "annual_ttl_tax_sal_ser_val", precision = 16, scale = 2)
    private BigDecimal annualTtlTaxSalSerVal;

    @Column(name = "is_selected_audit", nullable = false)
    private boolean selectedAudit;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "create_by_id")
    private Long createById;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    @Column(name = "update_by_id")
    private Long updateById;

    @Column(name = "update_date")
    private LocalDateTime updateDate;
}
