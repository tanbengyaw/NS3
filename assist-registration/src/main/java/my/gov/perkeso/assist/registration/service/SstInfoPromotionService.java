package my.gov.perkeso.assist.registration.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import my.gov.perkeso.assist.registration.constant.RegistrationSectionRouting;
import my.gov.perkeso.assist.registration.constant.SstContractType;
import my.gov.perkeso.assist.registration.constant.SstStatus;
import my.gov.perkeso.assist.registration.constant.TaxType;
import my.gov.perkeso.assist.registration.domain.DirectorOwner;
import my.gov.perkeso.assist.registration.domain.DirectorOwnerRepository;
import my.gov.perkeso.assist.registration.domain.Employer;
import my.gov.perkeso.assist.registration.domain.Premises;
import my.gov.perkeso.assist.registration.domain.PremisesRepository;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.SstInfo;
import my.gov.perkeso.assist.registration.domain.SstInfoRepository;
import my.gov.perkeso.assist.registration.domain.SstStatusInfo;
import my.gov.perkeso.assist.registration.domain.SstStatusInfoRepository;
import my.gov.perkeso.assist.registration.domain.SstSupportingDocument;
import my.gov.perkeso.assist.registration.domain.SstSupportingDocumentRepository;
import my.gov.perkeso.assist.registration.domain.SstServiceCategory;
import my.gov.perkeso.assist.registration.domain.SstServiceCategoryRepository;
import my.gov.perkeso.assist.registration.domain.SstTariffCode;
import my.gov.perkeso.assist.registration.domain.SstTariffCodeRepository;
import my.gov.perkeso.assist.registration.domain.TempDirectorOwner;
import my.gov.perkeso.assist.registration.domain.TempPremises;
import my.gov.perkeso.assist.registration.domain.TempSstInfo;
import my.gov.perkeso.assist.registration.domain.TempSstServiceCategory;
import my.gov.perkeso.assist.registration.domain.TempSstSupportingDocument;
import my.gov.perkeso.assist.registration.domain.TempSstTariffCode;
import my.gov.perkeso.assist.registration.employercode.EmployerCodeContext;
import my.gov.perkeso.assist.registration.employercode.EmployerCodeGeneratorFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SstInfoPromotionService {

    private final TempSstInfoWritePlatformService tempSstInfoWritePlatformService;
    private final SstInfoRepository sstInfoRepository;
    private final SstStatusInfoRepository sstStatusInfoRepository;
    private final SstTariffCodeRepository sstTariffCodeRepository;
    private final SstServiceCategoryRepository sstServiceCategoryRepository;
    private final SstSupportingDocumentRepository sstSupportingDocumentRepository;
    private final DirectorOwnerRepository directorOwnerRepository;
    private final PremisesRepository premisesRepository;
    private final EmployerCodeGeneratorFactory employerCodeGeneratorFactory;
    private final RegistrationDocumentStorageService registrationDocumentStorageService;

    @Transactional
    public SstInfo promoteSstOnApprove(final RegGeneralInfo regCase, final Employer employer) {
        if (!RegistrationSectionRouting.isSstNewRegSection(regCase.getSectionId())) {
            return null;
        }

        final List<SstInfo> existingForCase = sstInfoRepository.findByRegGeneralInfoIdAndDeletedFalse(regCase.getId());
        if (!existingForCase.isEmpty()) {
            return existingForCase.get(existingForCase.size() - 1);
        }

        final TempSstInfo tempSstInfo = tempSstInfoWritePlatformService.requireTempSstInfoForCase(regCase);
        final List<TempDirectorOwner> directors = tempSstInfoWritePlatformService
                .listDirectorsForCase(regCase.getId());
        final List<TempPremises> premises = tempSstInfoWritePlatformService.listPremisesForCase(regCase.getId());
        final List<TempSstTariffCode> tariffCodes = tempSstInfoWritePlatformService
                .listTariffCodesForCase(tempSstInfo);
        final List<TempSstServiceCategory> serviceCategories = tempSstInfoWritePlatformService
                .listServiceCategoriesForCase(tempSstInfo);
        final List<TempSstSupportingDocument> supportingDocuments = tempSstInfoWritePlatformService
                .listSupportingDocumentEntitiesForCase(tempSstInfo);

        final EmployerCodeContext context = EmployerCodeContext.builder()
                .branchId(regCase.getTempEmployer().getPksBranchId())
                .postCode(regCase.getTempEmployer().getPostCode())
                .build();
        final RegistrationSection section = RegistrationSection.fromAssistSectionId(regCase.getSectionId());
        final TaxType taxType = RegistrationSectionRouting.taxTypeForSstNewReg(regCase.getSectionId());
        final String smkNo = employerCodeGeneratorFactory.generateSmkNo(section, context);

        final SstInfo sstInfo = mapSstInfo(tempSstInfo, employer.getId(), regCase.getId(), smkNo, taxType);
        final SstInfo savedSstInfo = sstInfoRepository.save(sstInfo);

        promoteDirectors(employer.getId(), directors);
        promotePremises(employer.getId(), premises);
        promoteTariffCodes(employer.getId(), savedSstInfo.getId(), tariffCodes);
        promoteServiceCategories(employer.getId(), savedSstInfo.getId(), serviceCategories);
        promoteSupportingDocuments(regCase.getId(), employer.getId(), savedSstInfo.getId(), supportingDocuments);
        createActiveStatus(savedSstInfo.getId(), taxType);

        return savedSstInfo;
    }

    /**
     * Update Tax Payer promotion (sections 1200-1204): updates the employer's EXISTING SstInfo
     * (same id, same SMK columns) in place from the case's temp draft, then replaces (soft-delete +
     * recreate) directors/premises/tariff codes/service categories with the temp draft's rows.
     * Does not call {@link #createActiveStatus}: the tax type is already active, and no new SMK
     * number is generated.
     */
    @Transactional
    public SstInfo promoteSstOnUpdateApprove(final RegGeneralInfo regCase, final Employer employer) {
        final SstInfo sstInfo = sstInfoRepository.findFirstByEmployerIdAndDeletedFalseOrderByIdDesc(employer.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cannot update tax payer: no active SST record found for employer " + employer.getId()));

        final TempSstInfo tempSstInfo = tempSstInfoWritePlatformService.requireTempSstInfoForCase(regCase);
        applyUpdateFields(sstInfo, tempSstInfo);
        final SstInfo savedSstInfo = sstInfoRepository.save(sstInfo);

        final List<TempDirectorOwner> directors = tempSstInfoWritePlatformService
                .listDirectorsForCase(regCase.getId());
        final List<TempPremises> premises = tempSstInfoWritePlatformService.listPremisesForCase(regCase.getId());
        final List<TempSstTariffCode> tariffCodes = tempSstInfoWritePlatformService
                .listTariffCodesForCase(tempSstInfo);
        final List<TempSstServiceCategory> serviceCategories = tempSstInfoWritePlatformService
                .listServiceCategoriesForCase(tempSstInfo);
        final List<TempSstSupportingDocument> supportingDocuments = tempSstInfoWritePlatformService
                .listSupportingDocumentEntitiesForCase(tempSstInfo);

        replaceDirectors(employer.getId(), directors);
        replacePremises(employer.getId(), premises);
        replaceTariffCodes(employer.getId(), savedSstInfo.getId(), tariffCodes);
        replaceServiceCategories(employer.getId(), savedSstInfo.getId(), serviceCategories);
        promoteSupportingDocuments(regCase.getId(), employer.getId(), savedSstInfo.getId(), supportingDocuments);

        return savedSstInfo;
    }

    private static void applyUpdateFields(final SstInfo sstInfo, final TempSstInfo temp) {
        sstInfo.setTradeName(temp.getTradeName());
        sstInfo.setTourTaxRegNo(temp.getTourTaxRegNo());
        sstInfo.setMotacRegNo(temp.getMotacRegNo());
        sstInfo.setLabuan(temp.isLabuan());
        sstInfo.setForm1ContactPerson(temp.getForm1ContactPerson());
        sstInfo.setWebsiteAddress(temp.getWebsiteAddress());
        sstInfo.setInTaxRefNo(temp.getInTaxRefNo());
        sstInfo.setCusAudRefNo(temp.getCusAudRefNo());
        sstInfo.setPreRegNo(temp.getPreRegNo());
        sstInfo.setPreRegName(temp.getPreRegName());
        sstInfo.setDateOfReplacement(temp.getDateOfReplacement());
        sstInfo.setManComDate(temp.getManComDate());
        sstInfo.setDateSaleValTaxGoods(temp.getDateSaleValTaxGoods());
        sstInfo.setFinYrEndMon(temp.getFinYrEndMon());
        sstInfo.setAnTotalTaxSalesVal(temp.getAnTotalTaxSalesVal());
        sstInfo.setBusinessComDate(temp.getBusinessComDate());
        sstInfo.setLocalSales(temp.getLocalSales());
        sstInfo.setExportSales(temp.getExportSales());
        sstInfo.setSalesToDesignArea(temp.getSalesToDesignArea());
        sstInfo.setOthersSales(temp.getOthersSales());
        sstInfo.setSubContractWork(temp.isSubContractWork());
        sstInfo.setDeclareTrue(temp.isDeclareTrue());
        sstInfo.setDeclareDate(temp.getDeclareDate());
        sstInfo.setApplicantName(temp.getApplicantName());
        sstInfo.setIdentityCard(temp.getIdentityCard());
        sstInfo.setDesignation(temp.getDesignation());
        sstInfo.setApplicantEmail(temp.getApplicantEmail());
        sstInfo.setApplicantTelNo(temp.getApplicantTelNo());
        sstInfo.setDsTypeSoftwareAppsGame(temp.isDsTypeSoftwareAppsGame());
        sstInfo.setDsTypeMusicEbookFilm(temp.isDsTypeMusicEbookFilm());
        sstInfo.setDsTypeAdOnlinePlatform(temp.isDsTypeAdOnlinePlatform());
        sstInfo.setDsTypeSearchEngineSocialNetwork(temp.isDsTypeSearchEngineSocialNetwork());
        sstInfo.setDsTypeDatabaseHosting(temp.isDsTypeDatabaseHosting());
        sstInfo.setDsTypeInternetBasedTelecom(temp.isDsTypeInternetBasedTelecom());
        sstInfo.setDsTypeOnlineTraining(temp.isDsTypeOnlineTraining());
        sstInfo.setDsTypeOthers(temp.isDsTypeOthers());
        sstInfo.setAchievingValueOfDsDate(temp.getAchievingValueOfDsDate());
        sstInfo.setDsTotalValue(temp.getDsTotalValue());
    }

    private void replaceDirectors(final Long employerId, final List<TempDirectorOwner> directors) {
        directorOwnerRepository.findByEmployerIdAndDeletedFalseOrderByIdAsc(employerId).forEach(existing -> {
            existing.setDeleted(true);
            directorOwnerRepository.save(existing);
        });
        promoteDirectors(employerId, directors);
    }

    private void replacePremises(final Long employerId, final List<TempPremises> premises) {
        premisesRepository.findByEmployerIdAndDeletedFalseOrderByIdAsc(employerId).forEach(existing -> {
            existing.setDeleted(true);
            premisesRepository.save(existing);
        });
        promotePremises(employerId, premises);
    }

    private void replaceTariffCodes(final Long employerId, final Long sstInfoId,
            final List<TempSstTariffCode> tariffCodes) {
        sstTariffCodeRepository.findByEmployerIdAndDeletedFalseOrderByIdAsc(employerId).forEach(existing -> {
            existing.setDeleted(true);
            sstTariffCodeRepository.save(existing);
        });
        promoteTariffCodes(employerId, sstInfoId, tariffCodes);
    }

    private void replaceServiceCategories(final Long employerId, final Long sstInfoId,
            final List<TempSstServiceCategory> serviceCategories) {
        sstServiceCategoryRepository.findByEmployerIdAndDeletedFalseOrderByIdAsc(employerId).forEach(existing -> {
            existing.setDeleted(true);
            sstServiceCategoryRepository.save(existing);
        });
        promoteServiceCategories(employerId, sstInfoId, serviceCategories);
    }

    private static SstInfo mapSstInfo(final TempSstInfo temp, final Long employerId, final Long caseId,
            final String smkNo, final TaxType taxType) {
        final SstInfo sstInfo = new SstInfo();
        sstInfo.setEmployerId(employerId);
        sstInfo.setRegGeneralInfoId(caseId);
        sstInfo.applyTaxSpecificSmk(taxType, smkNo);
        sstInfo.setTradeName(temp.getTradeName());
        sstInfo.setTourTaxRegNo(temp.getTourTaxRegNo());
        sstInfo.setMotacRegNo(temp.getMotacRegNo());
        sstInfo.setLabuan(temp.isLabuan());
        sstInfo.setForm1ContactPerson(temp.getForm1ContactPerson());
        sstInfo.setWebsiteAddress(temp.getWebsiteAddress());
        sstInfo.setInTaxRefNo(temp.getInTaxRefNo());
        sstInfo.setCusAudRefNo(temp.getCusAudRefNo());
        sstInfo.setPreRegNo(temp.getPreRegNo());
        sstInfo.setPreRegName(temp.getPreRegName());
        sstInfo.setDateOfReplacement(temp.getDateOfReplacement());
        sstInfo.setManComDate(temp.getManComDate());
        sstInfo.setDateSaleValTaxGoods(temp.getDateSaleValTaxGoods());
        sstInfo.setFinYrEndMon(temp.getFinYrEndMon());
        sstInfo.setAnTotalTaxSalesVal(temp.getAnTotalTaxSalesVal());
        sstInfo.setBusinessComDate(temp.getBusinessComDate());
        sstInfo.setLocalSales(temp.getLocalSales());
        sstInfo.setExportSales(temp.getExportSales());
        sstInfo.setSalesToDesignArea(temp.getSalesToDesignArea());
        sstInfo.setOthersSales(temp.getOthersSales());
        sstInfo.setSubContractWork(temp.isSubContractWork());
        sstInfo.setDeclareTrue(temp.isDeclareTrue());
        sstInfo.setDeclareDate(temp.getDeclareDate());
        sstInfo.setApplicantName(temp.getApplicantName());
        sstInfo.setIdentityCard(temp.getIdentityCard());
        sstInfo.setDesignation(temp.getDesignation());
        sstInfo.setApplicantEmail(temp.getApplicantEmail());
        sstInfo.setApplicantTelNo(temp.getApplicantTelNo());
        sstInfo.setDsTypeSoftwareAppsGame(temp.isDsTypeSoftwareAppsGame());
        sstInfo.setDsTypeMusicEbookFilm(temp.isDsTypeMusicEbookFilm());
        sstInfo.setDsTypeAdOnlinePlatform(temp.isDsTypeAdOnlinePlatform());
        sstInfo.setDsTypeSearchEngineSocialNetwork(temp.isDsTypeSearchEngineSocialNetwork());
        sstInfo.setDsTypeDatabaseHosting(temp.isDsTypeDatabaseHosting());
        sstInfo.setDsTypeInternetBasedTelecom(temp.isDsTypeInternetBasedTelecom());
        sstInfo.setDsTypeOnlineTraining(temp.isDsTypeOnlineTraining());
        sstInfo.setDsTypeOthers(temp.isDsTypeOthers());
        sstInfo.setAchievingValueOfDsDate(temp.getAchievingValueOfDsDate());
        sstInfo.setDsTotalValue(temp.getDsTotalValue());
        sstInfo.setAutoRegistration(false);
        sstInfo.setDeleted(false);
        sstInfo.setCreatedDate(LocalDateTime.now());
        return sstInfo;
    }

    private void promoteDirectors(final Long employerId, final List<TempDirectorOwner> directors) {
        for (final TempDirectorOwner temp : directors) {
            final DirectorOwner director = new DirectorOwner();
            director.setEmployerId(employerId);
            director.setName(temp.getName());
            director.setIdentificationTypeId(temp.getIdentificationTypeId());
            director.setIdentificationNo(temp.getIdentificationNo());
            director.setEmail(temp.getEmail());
            director.setDesignation(temp.getDesignation());
            director.setTelephoneNo(temp.getTelephoneNo());
            director.setDeleted(false);
            director.setCreatedDate(LocalDateTime.now());
            directorOwnerRepository.save(director);
        }
    }

    private void promotePremises(final Long employerId, final List<TempPremises> premises) {
        for (final TempPremises temp : premises) {
            final Premises row = new Premises();
            row.setEmployerId(employerId);
            row.setName(temp.getName());
            row.setAddressLine(temp.getAddressLine());
            row.setAddressLine2(temp.getAddressLine2());
            row.setAddressLine3(temp.getAddressLine3());
            row.setPostCode(temp.getPostCode());
            row.setCityName(temp.getCityName());
            row.setStateName(temp.getStateName());
            row.setDeleted(false);
            row.setCreatedDate(LocalDateTime.now());
            premisesRepository.save(row);
        }
    }

    private void promoteTariffCodes(final Long employerId, final Long sstInfoId,
            final List<TempSstTariffCode> tariffCodes) {
        for (final TempSstTariffCode temp : tariffCodes) {
            final SstTariffCode tariff = new SstTariffCode();
            tariff.setSstInfoId(sstInfoId);
            tariff.setEmployerId(employerId);
            tariff.setTariffCodeSalesTypeId(temp.getTariffCodeSalesTypeId());
            tariff.setContractTypeId(temp.getContractTypeId());
            tariff.setFinishedGoods(temp.getFinishedGoods());
            tariff.setDeleted(false);
            tariff.setCreatedDate(LocalDateTime.now());
            sstTariffCodeRepository.save(tariff);
        }
    }

    private void promoteServiceCategories(final Long employerId, final Long sstInfoId,
            final List<TempSstServiceCategory> serviceCategories) {
        for (final TempSstServiceCategory temp : serviceCategories) {
            final SstServiceCategory category = new SstServiceCategory();
            category.setSstInfoId(sstInfoId);
            category.setEmployerId(employerId);
            category.setSstServiceTypeId(temp.getSstServiceTypeId());
            category.setRemark(temp.getRemark());
            category.setDeleted(false);
            category.setCreatedDate(LocalDateTime.now());
            sstServiceCategoryRepository.save(category);
        }
    }

    private void promoteSupportingDocuments(final Long caseId, final Long employerId, final Long sstInfoId,
            final List<TempSstSupportingDocument> supportingDocuments) {
        for (final TempSstSupportingDocument temp : supportingDocuments) {
            try {
                final RegistrationDocumentStorageService.StoredRegistrationDocument stored =
                        registrationDocumentStorageService.promoteDraftDocument(caseId, employerId,
                                temp.getStoredFileName(), temp.getFileName(), temp.getContentType(),
                                temp.getFileSize() != null ? temp.getFileSize() : 0L);
                final SstSupportingDocument document = new SstSupportingDocument();
                document.setSstInfoId(sstInfoId);
                document.setEmployerId(employerId);
                document.setDocumentTypeId(temp.getDocumentTypeId());
                document.setFileName(temp.getFileName());
                document.setStoredFileName(stored.storedFileName());
                document.setContentType(stored.contentType());
                document.setFileSize(stored.fileSize());
                document.setDeleted(false);
                document.setCreatedDate(LocalDateTime.now());
                sstSupportingDocumentRepository.save(document);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to promote supporting document " + temp.getFileName(), ex);
            }
        }
    }

    private void createActiveStatus(final Long sstInfoId, final TaxType taxType) {
        final SstStatusInfo statusInfo = new SstStatusInfo();
        statusInfo.setSstInfoId(sstInfoId);
        statusInfo.setTaxTypeId(taxType.getAssistId());
        statusInfo.setSstStatusId(SstStatus.ACTIVE.getAssistId());
        statusInfo.setStartDate(LocalDate.now());
        statusInfo.setCurrent(true);
        statusInfo.setDeleted(false);
        statusInfo.setCreatedDate(LocalDateTime.now());
        sstStatusInfoRepository.save(statusInfo);
    }

    static boolean hasMainContractTariff(final List<TempSstTariffCode> tariffCodes) {
        return tariffCodes.stream()
                .anyMatch(t -> t.getContractTypeId() == SstContractType.MAIN_CONTRACT.getAssistId());
    }

    static boolean hasSubContractTariff(final List<TempSstTariffCode> tariffCodes) {
        return tariffCodes.stream()
                .anyMatch(t -> t.getContractTypeId() == SstContractType.SUB_CONTRACT.getAssistId());
    }
}
