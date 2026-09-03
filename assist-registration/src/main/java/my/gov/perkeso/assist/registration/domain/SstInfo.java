package my.gov.perkeso.assist.registration.domain;

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
@Table(name = "sst_info", schema = "registration")
@Getter
@Setter
public class SstInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employer_id", nullable = false)
    private Long employerId;

    @Column(name = "reg_general_info_id")
    private Long regGeneralInfoId;

    @Column(name = "smk_reg_no", length = 50)
    private String smkRegNo;

    @Column(name = "sales_tax_smk_reg_no", length = 50)
    private String salesTaxSmkRegNo;

    @Column(name = "service_tax_smk_reg_no", length = 50)
    private String serviceTaxSmkRegNo;

    @Column(name = "tourism_tax_smk_reg_no", length = 50)
    private String tourismTaxSmkRegNo;

    @Column(name = "digital_tax_smk_reg_no", length = 50)
    private String digitalTaxSmkRegNo;

    @Column(name = "dpsp_tax_smk_reg_no", length = 50)
    private String dpspTaxSmkRegNo;

    @Column(name = "trade_name", length = 100)
    private String tradeName;

    @Column(name = "tour_tax_reg_no", length = 50)
    private String tourTaxRegNo;

    @Column(name = "in_tax_ref_no", length = 50)
    private String inTaxRefNo;

    @Column(name = "cus_aud_ref_no", length = 50)
    private String cusAudRefNo;

    @Column(name = "pre_reg_no", length = 50)
    private String preRegNo;

    @Column(name = "pre_reg_name", length = 100)
    private String preRegName;

    @Column(name = "date_of_replacement")
    private LocalDate dateOfReplacement;

    @Column(name = "man_com_date")
    private LocalDate manComDate;

    @Column(name = "date_sale_val_tax_goods")
    private LocalDate dateSaleValTaxGoods;

    @Column(name = "fin_yr_end_mon")
    private Integer finYrEndMon;

    @Column(name = "an_total_tax_sales_val", precision = 18, scale = 2)
    private BigDecimal anTotalTaxSalesVal;

    @Column(name = "business_com_date")
    private LocalDate businessComDate;

    @Column(name = "local_sales", precision = 18, scale = 2)
    private BigDecimal localSales;

    @Column(name = "export_sales", precision = 18, scale = 2)
    private BigDecimal exportSales;

    @Column(name = "sales_to_design_area", precision = 18, scale = 2)
    private BigDecimal salesToDesignArea;

    @Column(name = "others_sales", precision = 18, scale = 2)
    private BigDecimal othersSales;

    @Column(name = "sub_contract_work", nullable = false)
    private boolean subContractWork;

    @Column(name = "declare_true", nullable = false)
    private boolean declareTrue;

    @Column(name = "declare_date")
    private LocalDate declareDate;

    @Column(name = "applicant_name", length = 100)
    private String applicantName;

    @Column(name = "identity_card", length = 30)
    private String identityCard;

    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "applicant_email", length = 100)
    private String applicantEmail;

    @Column(name = "applicant_tel_no", length = 30)
    private String applicantTelNo;

    @Column(name = "is_auto_registration", nullable = false)
    private boolean autoRegistration;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}
