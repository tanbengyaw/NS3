package my.gov.perkeso.assist.registration.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TempSstInfoData {

    Long id;
    Long caseId;
    String tradeName;
    String tourTaxRegNo;
    String inTaxRefNo;
    String cusAudRefNo;
    String preRegNo;
    String preRegName;
    LocalDate dateOfReplacement;
    LocalDate manComDate;
    LocalDate dateSaleValTaxGoods;
    Integer finYrEndMon;
    BigDecimal anTotalTaxSalesVal;
    LocalDate businessComDate;
    BigDecimal localSales;
    BigDecimal exportSales;
    BigDecimal salesToDesignArea;
    BigDecimal othersSales;
    boolean subContractWork;
    boolean declareTrue;
    LocalDate declareDate;
    String applicantName;
    String identityCard;
    String designation;
    String applicantEmail;
    String applicantTelNo;
    List<TempDirectorOwnerData> directors;
    List<TempPremisesData> premises;
    List<TempSstTariffCodeData> tariffCodes;
    List<TempSstContactPersonData> contactPersons;
    List<TempSstSupportingDocumentData> supportingDocuments;
}
