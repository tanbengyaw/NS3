package my.gov.perkeso.assist.registration.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.infrastructure.data.CommandProcessingResult;
import my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException;
import my.gov.perkeso.assist.core.security.PlatformUser;
import my.gov.perkeso.assist.core.security.PlatformUserContext;
import my.gov.perkeso.assist.registration.constant.DataSource;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import my.gov.perkeso.assist.registration.constant.RegistrationSectionRouting;
import my.gov.perkeso.assist.registration.constant.TaxType;
import my.gov.perkeso.assist.registration.domain.AppStatus;
import my.gov.perkeso.assist.registration.domain.BusinessInfo;
import my.gov.perkeso.assist.registration.domain.DirectorOwner;
import my.gov.perkeso.assist.registration.domain.DirectorOwnerRepository;
import my.gov.perkeso.assist.registration.domain.Employer;
import my.gov.perkeso.assist.registration.domain.EmployerRepository;
import my.gov.perkeso.assist.registration.domain.Premises;
import my.gov.perkeso.assist.registration.domain.PremisesRepository;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfoRepository;
import my.gov.perkeso.assist.registration.domain.SstInfo;
import my.gov.perkeso.assist.registration.domain.SstInfoRepository;
import my.gov.perkeso.assist.registration.domain.SstServiceCategory;
import my.gov.perkeso.assist.registration.domain.SstServiceCategoryRepository;
import my.gov.perkeso.assist.registration.domain.SstTariffCode;
import my.gov.perkeso.assist.registration.domain.SstTariffCodeRepository;
import my.gov.perkeso.assist.registration.domain.TempDirectorOwner;
import my.gov.perkeso.assist.registration.domain.TempDirectorOwnerRepository;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import my.gov.perkeso.assist.registration.domain.TempEmployerRepository;
import my.gov.perkeso.assist.registration.domain.TempPremises;
import my.gov.perkeso.assist.registration.domain.TempPremisesRepository;
import my.gov.perkeso.assist.registration.domain.TempSstInfo;
import my.gov.perkeso.assist.registration.domain.TempSstInfoRepository;
import my.gov.perkeso.assist.registration.domain.TempSstServiceCategory;
import my.gov.perkeso.assist.registration.domain.TempSstServiceCategoryRepository;
import my.gov.perkeso.assist.registration.domain.TempSstTariffCode;
import my.gov.perkeso.assist.registration.domain.TempSstTariffCodeRepository;
import my.gov.perkeso.assist.registration.reference.ReferenceNoGeneratorFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Starts an incomplete auto-reg completion case (1205–1209): clones the live employer + partial
 * {@code SstInfo} into a temp draft for the existing SST wizard.
 */
@Service
@RequiredArgsConstructor
public class IncompleteAutoRegCaseService {

    private static final Set<AppStatus> OPEN_STATUSES = Set.of(AppStatus.NEW, AppStatus.IN_PROGRESS,
            AppStatus.IN_QUERY, AppStatus.SUBMITTED);

    private final EmployerRepository employerRepository;
    private final SstInfoRepository sstInfoRepository;
    private final DirectorOwnerRepository directorOwnerRepository;
    private final PremisesRepository premisesRepository;
    private final SstTariffCodeRepository sstTariffCodeRepository;
    private final SstServiceCategoryRepository sstServiceCategoryRepository;
    private final RegGeneralInfoRepository regGeneralInfoRepository;
    private final TempEmployerRepository tempEmployerRepository;
    private final TempSstInfoRepository tempSstInfoRepository;
    private final TempDirectorOwnerRepository tempDirectorOwnerRepository;
    private final TempPremisesRepository tempPremisesRepository;
    private final TempSstTariffCodeRepository tempSstTariffCodeRepository;
    private final TempSstServiceCategoryRepository tempSstServiceCategoryRepository;
    private final ReferenceNoGeneratorFactory referenceNoGeneratorFactory;
    private final PlatformUserContext platformUserContext;

    @Transactional
    public CommandProcessingResult startCompletion(final Long sstInfoId) {
        if (sstInfoId == null) {
            throw new IllegalArgumentException("sstInfoId is required");
        }
        final PlatformUser currentUser = platformUserContext.getCurrentUser();
        if (currentUser.isEmployer()) {
            throw new IllegalArgumentException("Incomplete auto-registration is staff-only");
        }

        final SstInfo sstInfo = sstInfoRepository.findById(sstInfoId)
                .orElseThrow(() -> new ResourceNotFoundException("SST info not found: " + sstInfoId));
        if (sstInfo.isDeleted() || !sstInfo.isAutoRegistration()) {
            throw new IllegalArgumentException("SST row " + sstInfoId + " is not an auto-registration record");
        }
        final TaxType taxType = IncompleteAutoRegCompleteness.parseTaxType(sstInfo.getTaxType());
        if (IncompleteAutoRegCompleteness.isComplete(taxType, sstInfo)) {
            throw new IllegalArgumentException("SST row " + sstInfoId + " is already complete");
        }

        final Optional<RegGeneralInfo> open = regGeneralInfoRepository
                .findFirstBySourceSstInfoIdAndAppStatusIn(sstInfoId, OPEN_STATUSES);
        if (open.isPresent()) {
            return CommandProcessingResult.resourceResult(open.get().getId(), open.get().getCaseRefNo());
        }

        final Employer employer = employerRepository.findById(sstInfo.getEmployerId())
                .orElseThrow(() -> new ResourceNotFoundException("Employer not found: " + sstInfo.getEmployerId()));
        final Optional<TempEmployer> addressSource = resolveAddressSource(sstInfo);
        final long sectionId = RegistrationSectionRouting.incompleteSectionIdFor(taxType);
        final RegistrationSection section = RegistrationSection.fromAssistSectionId(sectionId);

        final TempEmployer tempEmployer = cloneEmployerToTempEmployer(employer, addressSource);
        final RegGeneralInfo regCase = new RegGeneralInfo();
        regCase.setCaseRefNo(referenceNoGeneratorFactory.generateForSection(section));
        regCase.setAppStatus(AppStatus.NEW);
        regCase.setRegType(section.name());
        regCase.setSectionId(section.getAssistSectionId());
        regCase.setDataSourceId(DataSource.OTC.getAssistId());
        regCase.setPksBranchId(employer.getPksBranchId());
        regCase.setProcessingPksBranchId(employer.getPksBranchId());
        regCase.setReceivingPksBranchId(employer.getPksBranchId());
        regCase.setTempEmployer(tempEmployer);
        regCase.setEmployerId(employer.getId());
        regCase.setSourceSstInfoId(sstInfo.getId());
        regCase.setCreatedByUsername(currentUser.username());
        regCase.setCreatedDate(LocalDateTime.now());
        regCase.setDocumentReceivedDate(LocalDateTime.now());
        final RegGeneralInfo savedCase = regGeneralInfoRepository.save(regCase);

        final TempSstInfo savedTempSstInfo = tempSstInfoRepository
                .save(cloneSstInfoToTempSstInfo(sstInfo, savedCase.getTempEmployer().getId()));
        cloneDirectors(savedCase.getId(),
                directorOwnerRepository.findByEmployerIdAndDeletedFalseOrderByIdAsc(employer.getId()));
        clonePremises(savedCase.getId(),
                premisesRepository.findByEmployerIdAndDeletedFalseOrderByIdAsc(employer.getId()));
        cloneTariffCodes(savedTempSstInfo.getId(),
                sstTariffCodeRepository.findByEmployerIdAndDeletedFalseOrderByIdAsc(employer.getId()));
        cloneServiceCategories(savedTempSstInfo.getId(),
                sstServiceCategoryRepository.findByEmployerIdAndDeletedFalseOrderByIdAsc(employer.getId()));

        return CommandProcessingResult.resourceResult(savedCase.getId(), savedCase.getCaseRefNo());
    }

    private Optional<TempEmployer> resolveAddressSource(final SstInfo sstInfo) {
        if (sstInfo.getIngestTempEmployerId() != null) {
            return tempEmployerRepository.findById(sstInfo.getIngestTempEmployerId());
        }
        if (sstInfo.getRegGeneralInfoId() == null) {
            return Optional.empty();
        }
        return regGeneralInfoRepository.findById(sstInfo.getRegGeneralInfoId()).map(RegGeneralInfo::getTempEmployer);
    }

    private static TempEmployer cloneEmployerToTempEmployer(final Employer employer,
            final Optional<TempEmployer> addressSource) {
        final BusinessInfo businessInfo = new BusinessInfo();
        businessInfo.setRegistrationNo(employer.getBusinessInfo().getRegistrationNo());
        businessInfo.setBusinessEntityTypeId(employer.getBusinessInfo().getBusinessEntityTypeId());

        final TempEmployer tempEmployer = new TempEmployer();
        tempEmployer.setEmployerName(employer.getEmployerName());
        tempEmployer.setBusinessInfo(businessInfo);
        tempEmployer.setServiceTypeId(employer.getServiceTypeId());
        tempEmployer.setPksBranchId(employer.getPksBranchId());
        tempEmployer.setBranch(employer.isBranch());
        tempEmployer.setMsicId(employer.getMsicId());
        tempEmployer.setMethodContributionPaymentId(employer.getMethodContributionPaymentId());
        tempEmployer.setEmployerRegisterStatus(employer.getEmployerRegisterStatus());
        tempEmployer.setEmail(employer.getEmail());
        tempEmployer.setPhone(employer.getPhone());
        addressSource.ifPresent(source -> {
            tempEmployer.setPostCode(source.getPostCode());
            tempEmployer.setContactPhones(source.getContactPhones());
            tempEmployer.setContactFaxes(source.getContactFaxes());
            tempEmployer.setAddressLine1(source.getAddressLine1());
            tempEmployer.setAddressLine2(source.getAddressLine2());
            tempEmployer.setAddressLine3(source.getAddressLine3());
            tempEmployer.setStateId(source.getStateId());
            tempEmployer.setCityId(source.getCityId());
            tempEmployer.setCityName(source.getCityName());
            tempEmployer.setCorrAddressLine1(source.getCorrAddressLine1());
            tempEmployer.setCorrAddressLine2(source.getCorrAddressLine2());
            tempEmployer.setCorrAddressLine3(source.getCorrAddressLine3());
            tempEmployer.setCorrPostCode(source.getCorrPostCode());
            tempEmployer.setCorrStateId(source.getCorrStateId());
            tempEmployer.setCorrCityId(source.getCorrCityId());
            tempEmployer.setCorrCityName(source.getCorrCityName());
        });
        return tempEmployer;
    }

    private static TempSstInfo cloneSstInfoToTempSstInfo(final SstInfo sstInfo, final Long tempEmployerId) {
        final TempSstInfo temp = new TempSstInfo();
        temp.setTempEmployerId(tempEmployerId);
        temp.setTradeName(sstInfo.getTradeName());
        temp.setTourTaxRegNo(sstInfo.getTourTaxRegNo());
        temp.setMotacRegNo(sstInfo.getMotacRegNo());
        temp.setLabuan(sstInfo.isLabuan());
        temp.setForm1ContactPerson(sstInfo.getForm1ContactPerson());
        temp.setWebsiteAddress(sstInfo.getWebsiteAddress());
        temp.setInTaxRefNo(sstInfo.getInTaxRefNo());
        temp.setCusAudRefNo(sstInfo.getCusAudRefNo());
        temp.setPreRegNo(sstInfo.getPreRegNo());
        temp.setPreRegName(sstInfo.getPreRegName());
        temp.setDateOfReplacement(sstInfo.getDateOfReplacement());
        temp.setManComDate(sstInfo.getManComDate());
        temp.setDateSaleValTaxGoods(sstInfo.getDateSaleValTaxGoods());
        temp.setFinYrEndMon(sstInfo.getFinYrEndMon());
        temp.setAnTotalTaxSalesVal(sstInfo.getAnTotalTaxSalesVal());
        temp.setBusinessComDate(sstInfo.getBusinessComDate());
        temp.setLocalSales(sstInfo.getLocalSales());
        temp.setExportSales(sstInfo.getExportSales());
        temp.setSalesToDesignArea(sstInfo.getSalesToDesignArea());
        temp.setOthersSales(sstInfo.getOthersSales());
        temp.setSubContractWork(sstInfo.isSubContractWork());
        temp.setDeclareTrue(sstInfo.isDeclareTrue());
        temp.setDeclareDate(sstInfo.getDeclareDate());
        temp.setApplicantName(sstInfo.getApplicantName());
        temp.setIdentityCard(sstInfo.getIdentityCard());
        temp.setDesignation(sstInfo.getDesignation());
        temp.setApplicantEmail(sstInfo.getApplicantEmail());
        temp.setApplicantTelNo(sstInfo.getApplicantTelNo());
        temp.setDsTypeSoftwareAppsGame(sstInfo.isDsTypeSoftwareAppsGame());
        temp.setDsTypeMusicEbookFilm(sstInfo.isDsTypeMusicEbookFilm());
        temp.setDsTypeAdOnlinePlatform(sstInfo.isDsTypeAdOnlinePlatform());
        temp.setDsTypeSearchEngineSocialNetwork(sstInfo.isDsTypeSearchEngineSocialNetwork());
        temp.setDsTypeDatabaseHosting(sstInfo.isDsTypeDatabaseHosting());
        temp.setDsTypeInternetBasedTelecom(sstInfo.isDsTypeInternetBasedTelecom());
        temp.setDsTypeOnlineTraining(sstInfo.isDsTypeOnlineTraining());
        temp.setDsTypeOthers(sstInfo.isDsTypeOthers());
        temp.setAchievingValueOfDsDate(sstInfo.getAchievingValueOfDsDate());
        temp.setDsTotalValue(sstInfo.getDsTotalValue());
        temp.setCreatedDate(LocalDateTime.now());
        return temp;
    }

    private void cloneDirectors(final Long caseId, final List<DirectorOwner> directors) {
        for (final DirectorOwner source : directors) {
            final TempDirectorOwner clone = new TempDirectorOwner();
            clone.setRegGeneralInfoId(caseId);
            clone.setName(source.getName());
            clone.setIdentificationTypeId(source.getIdentificationTypeId());
            clone.setIdentificationNo(source.getIdentificationNo());
            clone.setEmail(source.getEmail());
            clone.setDesignation(source.getDesignation());
            clone.setTelephoneNo(source.getTelephoneNo());
            clone.setDeleted(false);
            clone.setCreatedDate(LocalDateTime.now());
            tempDirectorOwnerRepository.save(clone);
        }
    }

    private void clonePremises(final Long caseId, final List<Premises> premisesList) {
        for (final Premises source : premisesList) {
            final TempPremises clone = new TempPremises();
            clone.setRegGeneralInfoId(caseId);
            clone.setName(source.getName());
            clone.setAddressLine(source.getAddressLine());
            clone.setAddressLine2(source.getAddressLine2());
            clone.setAddressLine3(source.getAddressLine3());
            clone.setPostCode(source.getPostCode());
            clone.setCityName(source.getCityName());
            clone.setStateName(source.getStateName());
            clone.setDeleted(false);
            clone.setCreatedDate(LocalDateTime.now());
            tempPremisesRepository.save(clone);
        }
    }

    private void cloneTariffCodes(final Long tempSstInfoId, final List<SstTariffCode> tariffCodes) {
        for (final SstTariffCode source : tariffCodes) {
            final TempSstTariffCode clone = new TempSstTariffCode();
            clone.setTempSstInfoId(tempSstInfoId);
            clone.setTariffCodeSalesTypeId(source.getTariffCodeSalesTypeId());
            clone.setContractTypeId(source.getContractTypeId());
            clone.setFinishedGoods(source.getFinishedGoods());
            clone.setDeleted(false);
            clone.setCreatedDate(LocalDateTime.now());
            tempSstTariffCodeRepository.save(clone);
        }
    }

    private void cloneServiceCategories(final Long tempSstInfoId, final List<SstServiceCategory> serviceCategories) {
        for (final SstServiceCategory source : serviceCategories) {
            final TempSstServiceCategory clone = new TempSstServiceCategory();
            clone.setTempSstInfoId(tempSstInfoId);
            clone.setSstServiceTypeId(source.getSstServiceTypeId());
            clone.setRemark(source.getRemark());
            clone.setDeleted(false);
            clone.setCreatedDate(LocalDateTime.now());
            tempSstServiceCategoryRepository.save(clone);
        }
    }
}
