package my.gov.perkeso.assist.registration.service;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.constant.DataSource;
import my.gov.perkeso.assist.registration.constant.RegistrationSectionRouting;
import my.gov.perkeso.assist.registration.constant.SstContractType;
import my.gov.perkeso.assist.registration.data.DiscontinueTaxInfoData;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import my.gov.perkeso.assist.registration.domain.TempSstInfo;
import my.gov.perkeso.assist.registration.domain.TempSstServiceCategory;
import my.gov.perkeso.assist.registration.domain.TempSstTariffCode;
import my.gov.perkeso.assist.registration.exception.RegistrationCaseSubmitValidationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegistrationCaseSubmitValidator {

    private final TempSstInfoWritePlatformService tempSstInfoWritePlatformService;
    private final DiscontinueTaxCaseService discontinueTaxCaseService;

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

        if (RegistrationSectionRouting.isSstSalesTaxNewReg(regCase.getSectionId())) {
            validateSalesTaxSubmit(regCase);
        } else if (RegistrationSectionRouting.isSstTourismTaxNewReg(regCase.getSectionId())) {
            validateTourismTaxSubmit(regCase);
        } else if (RegistrationSectionRouting.isSstDpspTaxNewReg(regCase.getSectionId())) {
            validateDpspTaxSubmit(regCase);
        } else if (RegistrationSectionRouting.isSstServiceTaxNewReg(regCase.getSectionId())) {
            validateServiceTaxSubmit(regCase);
        } else if (RegistrationSectionRouting.isSstDigitalTaxNewReg(regCase.getSectionId())) {
            validateDigitalTaxSubmit(regCase);
        } else if (RegistrationSectionRouting.isDiscontinueTaxSection(regCase.getSectionId())) {
            validateDiscontinueTaxSubmit(regCase);
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

        if (!tempSstInfo.isDeclareTrue()) {
            throw new RegistrationCaseSubmitValidationException(
                    "Part C declaration must be accepted before submit");
        }
        requireNonBlank(tempSstInfo.getApplicantName(), "applicantName");
    }

    private void validateTourismTaxSubmit(final RegGeneralInfo regCase) {
        final TempSstInfo tempSstInfo = tempSstInfoWritePlatformService.requireTempSstInfoForCase(regCase);
        requireNonNull(tempSstInfo.getFinYrEndMon(), "finYrEndMon");
        requireNonNull(tempSstInfo.getBusinessComDate(), "businessComDate");
        if (!tempSstInfo.isDeclareTrue()) {
            throw new RegistrationCaseSubmitValidationException(
                    "Part C declaration must be accepted before submit");
        }
        requireNonBlank(tempSstInfo.getApplicantName(), "applicantName");
        requireNonBlank(tempSstInfo.getForm1ContactPerson(), "form1ContactPerson");
    }

    private void validateServiceTaxSubmit(final RegGeneralInfo regCase) {
        final TempSstInfo tempSstInfo = tempSstInfoWritePlatformService.requireTempSstInfoForCase(regCase);

        requireNonNull(tempSstInfo.getManComDate(), "manComDate");
        requireNonNull(tempSstInfo.getDateSaleValTaxGoods(), "dateSaleValTaxGoods");
        requireNonNull(tempSstInfo.getFinYrEndMon(), "finYrEndMon");
        requireNonNull(tempSstInfo.getBusinessComDate(), "businessComDate");
        requireNonNull(tempSstInfo.getAnTotalTaxSalesVal(), "anTotalTaxSalesVal");

        final List<TempSstServiceCategory> serviceCategories =
                tempSstInfoWritePlatformService.listServiceCategoriesForCase(tempSstInfo);
        if (serviceCategories.isEmpty()) {
            throw new RegistrationCaseSubmitValidationException(
                    "At least one service type code is required before submit");
        }

        if (tempSstInfoWritePlatformService.listDirectorsForCase(regCase.getId()).isEmpty()) {
            throw new RegistrationCaseSubmitValidationException(
                    "At least one director is required before submit");
        }

        if (!tempSstInfo.isDeclareTrue()) {
            throw new RegistrationCaseSubmitValidationException(
                    "Part C declaration must be accepted before submit");
        }
        requireNonBlank(tempSstInfo.getApplicantName(), "applicantName");
    }

    private void validateDigitalTaxSubmit(final RegGeneralInfo regCase) {
        final TempSstInfo tempSstInfo = tempSstInfoWritePlatformService.requireTempSstInfoForCase(regCase);
        requireNonNull(tempSstInfo.getFinYrEndMon(), "finYrEndMon");
        requireNonNull(tempSstInfo.getBusinessComDate(), "businessComDate");
        requireNonNull(tempSstInfo.getAchievingValueOfDsDate(), "achievingValueOfDsDate");
        requireNonNull(tempSstInfo.getDsTotalValue(), "dsTotalValue");

        final boolean hasDigitalServiceType = tempSstInfo.isDsTypeSoftwareAppsGame()
                || tempSstInfo.isDsTypeMusicEbookFilm() || tempSstInfo.isDsTypeAdOnlinePlatform()
                || tempSstInfo.isDsTypeSearchEngineSocialNetwork() || tempSstInfo.isDsTypeDatabaseHosting()
                || tempSstInfo.isDsTypeInternetBasedTelecom() || tempSstInfo.isDsTypeOnlineTraining()
                || tempSstInfo.isDsTypeOthers();
        if (!hasDigitalServiceType) {
            throw new RegistrationCaseSubmitValidationException(
                    "At least one type of digital service is required before submit");
        }

        if (tempSstInfoWritePlatformService.listDirectorsForCase(regCase.getId()).isEmpty()) {
            throw new RegistrationCaseSubmitValidationException(
                    "At least one authorised personnel is required before submit");
        }

        if (!tempSstInfo.isDeclareTrue()) {
            throw new RegistrationCaseSubmitValidationException(
                    "Part C declaration must be accepted before submit");
        }
        requireNonBlank(tempSstInfo.getApplicantName(), "applicantName");
    }

    private void validateDiscontinueTaxSubmit(final RegGeneralInfo regCase) {
        final DiscontinueTaxInfoData info = discontinueTaxCaseService.getDiscontinueInfo(regCase.getId());
        requireNonNull(info.getNewSstStatusId(), "newSstStatusId");
        if (info.getNewSstStatusId() != null
                && info.getNewSstStatusId() == my.gov.perkeso.assist.registration.constant.SstStatus.CANCEL.getAssistId()) {
            requireNonNull(info.getCessationTaxEffectiveFrom(), "cessationTaxEffectiveFrom");
        }
    }

    private void validateDpspTaxSubmit(final RegGeneralInfo regCase) {
        final TempSstInfo tempSstInfo = tempSstInfoWritePlatformService.requireTempSstInfoForCase(regCase);
        requireNonNull(tempSstInfo.getFinYrEndMon(), "finYrEndMon");
        requireNonNull(tempSstInfo.getBusinessComDate(), "businessComDate");
        requireNonBlank(tempSstInfo.getWebsiteAddress(), "websiteAddress");
        requireNonBlank(regCase.getTempEmployer().getEmail(), "email");
        if (!tempSstInfo.isDeclareTrue()) {
            throw new RegistrationCaseSubmitValidationException(
                    "Part C declaration must be accepted before submit");
        }
        requireNonBlank(tempSstInfo.getApplicantName(), "applicantName");
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
