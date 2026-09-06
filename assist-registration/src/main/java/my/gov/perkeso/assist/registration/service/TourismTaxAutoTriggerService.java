package my.gov.perkeso.assist.registration.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import my.gov.perkeso.assist.registration.constant.RegistrationSectionRouting;
import my.gov.perkeso.assist.registration.data.SstServiceTypeData;
import my.gov.perkeso.assist.registration.domain.AppStatus;
import my.gov.perkeso.assist.registration.domain.BusinessInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfoRepository;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import my.gov.perkeso.assist.registration.domain.TempSstInfo;
import my.gov.perkeso.assist.registration.domain.TempSstInfoRepository;
import my.gov.perkeso.assist.registration.domain.TempSstServiceCategory;
import my.gov.perkeso.assist.registration.domain.TempSstServiceCategoryRepository;
import my.gov.perkeso.assist.registration.reference.ReferenceNoGeneratorFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mirrors ASSIST {@code RegUtility#checkAccomodationServiceAnnualTaxSaleLimit} and
 * {@code NewSstRegCounter}: a Service Tax registration whose service type includes
 * "Accommodation" and whose annual taxable sales exceed RM 500,000 must also auto-trigger a
 * linked Tourism Tax new-registration case, pre-populated from the same employer/business info.
 *
 * <p>The check is idempotent per case (tracked via {@code TempSstInfo#triggeredTourismCaseId})
 * so re-saving Form 2 or adding further service codes will not create duplicate tourism cases.
 */
@Service
@RequiredArgsConstructor
public class TourismTaxAutoTriggerService {

    private static final BigDecimal TOURISM_TAX_ANNUAL_SALES_LIMIT = new BigDecimal("500000.00");

    private final RegGeneralInfoRepository regGeneralInfoRepository;
    private final TempSstInfoRepository tempSstInfoRepository;
    private final TempSstServiceCategoryRepository tempSstServiceCategoryRepository;
    private final ReferenceNoGeneratorFactory referenceNoGeneratorFactory;
    private final RegistrationReferenceReadPlatformService registrationReferenceReadPlatformService;

    @Transactional
    public TempSstInfo checkAndTrigger(final RegGeneralInfo serviceTaxCase, final TempSstInfo tempSstInfo) {
        if (!RegistrationSectionRouting.isSstServiceTaxNewReg(serviceTaxCase.getSectionId())) {
            return tempSstInfo;
        }
        if (tempSstInfo.getTriggeredTourismCaseId() != null) {
            // Already triggered for this case — legacy IncompleteAutoRegTaxCounter only fires once.
            return tempSstInfo;
        }
        final BigDecimal annualTaxSale = tempSstInfo.getAnTotalTaxSalesVal();
        final boolean exceedsLimit = annualTaxSale != null
                && annualTaxSale.compareTo(TOURISM_TAX_ANNUAL_SALES_LIMIT) > 0;
        if (!exceedsLimit || !hasAccommodationServiceCategory(tempSstInfo)) {
            return tempSstInfo;
        }

        final RegGeneralInfo tourismCase = createLinkedTourismCase(serviceTaxCase);
        tempSstInfo.setTriggeredTourismCaseId(tourismCase.getId());
        tempSstInfo.setTriggeredTourismCaseRefNo(tourismCase.getCaseRefNo());
        return tempSstInfoRepository.save(tempSstInfo);
    }

    private boolean hasAccommodationServiceCategory(final TempSstInfo tempSstInfo) {
        if (tempSstInfo.getId() == null) {
            return false;
        }
        final List<TempSstServiceCategory> categories = tempSstServiceCategoryRepository
                .findByTempSstInfoIdAndDeletedFalseOrderByIdAsc(tempSstInfo.getId());
        if (categories.isEmpty()) {
            return false;
        }
        final Map<Long, SstServiceTypeData> lookup = registrationReferenceReadPlatformService
                .retrieveSstServiceTypeMap(categories.stream().map(TempSstServiceCategory::getSstServiceTypeId).toList());
        return categories.stream().anyMatch(category -> {
            final SstServiceTypeData type = lookup.get(category.getSstServiceTypeId());
            return type != null && type.isAccommodation();
        });
    }

    private RegGeneralInfo createLinkedTourismCase(final RegGeneralInfo serviceTaxCase) {
        final TempEmployer tourismEmployer = cloneTempEmployer(serviceTaxCase.getTempEmployer());

        final RegGeneralInfo tourismCase = new RegGeneralInfo();
        tourismCase.setCaseRefNo(
                referenceNoGeneratorFactory.generateForSection(RegistrationSection.REG_SST_TOURISM_TAX));
        tourismCase.setAppStatus(AppStatus.NEW);
        tourismCase.setRegType(RegistrationSection.REG_SST_TOURISM_TAX.name());
        tourismCase.setSectionId(RegistrationSection.REG_SST_TOURISM_TAX.getAssistSectionId());
        tourismCase.setDataSourceId(serviceTaxCase.getDataSourceId());
        tourismCase.setPksBranchId(serviceTaxCase.getPksBranchId());
        tourismCase.setProcessingPksBranchId(serviceTaxCase.getProcessingPksBranchId());
        tourismCase.setReceivingPksBranchId(serviceTaxCase.getReceivingPksBranchId());
        tourismCase.setTempEmployer(tourismEmployer);
        tourismCase.setCreatedByUsername(serviceTaxCase.getCreatedByUsername());
        tourismCase.setCreatedDate(LocalDateTime.now());
        tourismCase.setDocumentReceivedDate(serviceTaxCase.getDocumentReceivedDate());
        tourismCase.setLinkedCaseId(serviceTaxCase.getId());
        tourismCase.setLinkedCaseRefNo(serviceTaxCase.getCaseRefNo());
        return regGeneralInfoRepository.save(tourismCase);
    }

    private static TempEmployer cloneTempEmployer(final TempEmployer source) {
        final BusinessInfo businessInfo = new BusinessInfo();
        businessInfo.setRegistrationNo(source.getBusinessInfo().getRegistrationNo());
        businessInfo.setBusinessEntityTypeId(source.getBusinessInfo().getBusinessEntityTypeId());

        final TempEmployer clone = new TempEmployer();
        clone.setEmployerName(source.getEmployerName());
        clone.setBusinessInfo(businessInfo);
        clone.setServiceTypeId(source.getServiceTypeId());
        clone.setPksBranchId(source.getPksBranchId());
        clone.setBranch(source.isBranch());
        clone.setMsicId(source.getMsicId());
        clone.setMethodContributionPaymentId(source.getMethodContributionPaymentId());
        clone.setEmployerRegisterStatus(source.getEmployerRegisterStatus());
        clone.setPostCode(source.getPostCode());
        clone.setEmail(source.getEmail());
        clone.setPhone(source.getPhone());
        clone.setContactPhones(source.getContactPhones());
        clone.setContactFaxes(source.getContactFaxes());
        clone.setAddressLine1(source.getAddressLine1());
        clone.setAddressLine2(source.getAddressLine2());
        clone.setAddressLine3(source.getAddressLine3());
        clone.setStateId(source.getStateId());
        clone.setCityId(source.getCityId());
        clone.setCityName(source.getCityName());
        clone.setCorrAddressLine1(source.getCorrAddressLine1());
        clone.setCorrAddressLine2(source.getCorrAddressLine2());
        clone.setCorrAddressLine3(source.getCorrAddressLine3());
        clone.setCorrPostCode(source.getCorrPostCode());
        clone.setCorrStateId(source.getCorrStateId());
        clone.setCorrCityId(source.getCorrCityId());
        clone.setCorrCityName(source.getCorrCityName());
        return clone;
    }
}
