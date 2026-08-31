package my.gov.perkeso.assist.registration.service;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.constant.DataSource;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import my.gov.perkeso.assist.registration.constant.SstContractType;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import my.gov.perkeso.assist.registration.domain.TempSstInfo;
import my.gov.perkeso.assist.registration.domain.TempSstTariffCode;
import my.gov.perkeso.assist.registration.exception.RegistrationCaseSubmitValidationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegistrationCaseSubmitValidator {

    private final TempSstInfoWritePlatformService tempSstInfoWritePlatformService;

    public void validateForSubmit(final RegGeneralInfo regCase) {
        final TempEmployer tempEmployer = regCase.getTempEmployer();
        if (tempEmployer == null) {
            throw new RegistrationCaseSubmitValidationException("Temp employer is required before submit");
        }

        requireNonBlank(tempEmployer.getEmployerName(), "employerName");
        requireNonBlank(tempEmployer.getBusinessInfo().getRegistrationNo(), "registrationNo");
        requireNonNull(tempEmployer.getServiceTypeId(), "serviceTypeId");
        requireNonNull(tempEmployer.getPksBranchId(), "pksBranchId");
        requireNonBlank(tempEmployer.getPostCode(), "postCode");
        requireNonNull(tempEmployer.getBusinessInfo().getBusinessEntityTypeId(), "businessEntityTypeId");
        requireNonBlank(tempEmployer.getAddressLine1(), "addressLine1");

        if (DataSource.PORTAL.getAssistId() == regCase.getDataSourceId()) {
            requireNonBlank(tempEmployer.getEmail(), "email");
        }

        if (regCase.getSectionId() != null
                && regCase.getSectionId() == RegistrationSection.REG_NEW_REG_SST_SALES_TAX.getAssistSectionId()) {
            validateSalesTaxSubmit(regCase);
        }
    }

    private void validateSalesTaxSubmit(final RegGeneralInfo regCase) {
        final TempSstInfo tempSstInfo = tempSstInfoWritePlatformService.requireTempSstInfoForCase(regCase);

        requireNonNull(tempSstInfo.getAnTotalTaxSalesVal(), "anTotalTaxSalesVal");
        requireNonNull(tempSstInfo.getDateSaleValTaxGoods(), "dateSaleValTaxGoods");
        requireNonNull(tempSstInfo.getManComDate(), "manComDate");
        requireNonNull(tempSstInfo.getFinYrEndMon(), "finYrEndMon");
        requireNonNull(tempSstInfo.getBusinessComDate(), "businessComDate");
        requireNonNull(tempSstInfo.getLocalSales(), "localSales");
        requireNonNull(tempSstInfo.getExportSales(), "exportSales");
        requireNonNull(tempSstInfo.getSalesToDesignArea(), "salesToDesignArea");
        requireNonNull(tempSstInfo.getOthersSales(), "othersSales");

        final List<TempSstTariffCode> tariffCodes = tempSstInfoWritePlatformService
                .listTariffCodesForCase(tempSstInfo);
        if (!SstInfoPromotionService.hasMainContractTariff(tariffCodes)) {
            throw new RegistrationCaseSubmitValidationException(
                    "At least one main-contract tariff code is required before submit");
        }
        if (tempSstInfo.isSubContractWork() && !SstInfoPromotionService.hasSubContractTariff(tariffCodes)) {
            throw new RegistrationCaseSubmitValidationException(
                    "Sub-contract tariff codes are required when subContractWork is true");
        }

        if (tempSstInfoWritePlatformService.listDirectorsForCase(regCase.getId()).isEmpty()) {
            throw new RegistrationCaseSubmitValidationException(
                    "At least one director is required before submit");
        }
    }

    private static void requireNonBlank(final String value, final String field) {
        if (value == null || value.isBlank()) {
            throw new RegistrationCaseSubmitValidationException(field + " is required before submit");
        }
    }

    private static void requireNonNull(final Object value, final String field) {
        if (value == null) {
            throw new RegistrationCaseSubmitValidationException(field + " is required before submit");
        }
    }
}
