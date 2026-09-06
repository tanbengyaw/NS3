package my.gov.perkeso.assist.registration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException;
import my.gov.perkeso.assist.core.infrastructure.data.CommandProcessingResult;
import my.gov.perkeso.assist.core.security.PlatformUser;
import my.gov.perkeso.assist.core.security.PlatformUserContext;
import my.gov.perkeso.assist.registration.constant.DataSource;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import my.gov.perkeso.assist.registration.constant.RegistrationSectionRouting;
import my.gov.perkeso.assist.registration.data.TaxPayerUpdateDiffData;
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
 * Starts and diffs "Update Tax Payer" cases (sections 1200-1204): cloning a live employer's
 * current Employer/SstInfo/children into a fresh temp draft (reusing the existing SST new-reg
 * draft CRUD APIs for editing), and computing an on-the-fly changed-fields summary against the
 * live records for the preview step.
 */
@Service
@RequiredArgsConstructor
public class TaxPayerUpdateCaseService {

    private final EmployerRepository employerRepository;
    private final SstInfoRepository sstInfoRepository;
    private final DirectorOwnerRepository directorOwnerRepository;
    private final PremisesRepository premisesRepository;
    private final SstTariffCodeRepository sstTariffCodeRepository;
    private final SstServiceCategoryRepository sstServiceCategoryRepository;
    private final RegGeneralInfoRepository regGeneralInfoRepository;
    private final TempSstInfoRepository tempSstInfoRepository;
    private final TempDirectorOwnerRepository tempDirectorOwnerRepository;
    private final TempPremisesRepository tempPremisesRepository;
    private final TempSstTariffCodeRepository tempSstTariffCodeRepository;
    private final TempSstServiceCategoryRepository tempSstServiceCategoryRepository;
    private final ReferenceNoGeneratorFactory referenceNoGeneratorFactory;
    private final PlatformUserContext platformUserContext;
    private final ObjectMapper objectMapper;

    @Transactional
    public CommandProcessingResult startUpdate(final Long employerId, final Long sectionId) {
        if (!RegistrationSectionRouting.isUpdateTaxSection(sectionId)) {
            throw new IllegalArgumentException(
                    "sectionId must be one of the Update Tax Payer sections (1200-1204): " + sectionId);
        }
        if (employerId == null) {
            throw new IllegalArgumentException("employerId is required");
        }

        final Employer employer = loadEmployer(employerId);
        final SstInfo currentSstInfo = sstInfoRepository.findFirstByEmployerIdAndDeletedFalseOrderByIdDesc(employerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cannot update tax payer: employer " + employerId + " has no active SST record"));

        final List<DirectorOwner> directors = directorOwnerRepository
                .findByEmployerIdAndDeletedFalseOrderByIdAsc(employerId);
        final List<Premises> premisesList = premisesRepository.findByEmployerIdAndDeletedFalseOrderByIdAsc(employerId);
        final List<SstTariffCode> tariffCodes = sstTariffCodeRepository
                .findByEmployerIdAndDeletedFalseOrderByIdAsc(employerId);
        final List<SstServiceCategory> serviceCategories = sstServiceCategoryRepository
                .findByEmployerIdAndDeletedFalseOrderByIdAsc(employerId);

        final RegistrationSection section = RegistrationSection.fromAssistSectionId(sectionId);
        final PlatformUser currentUser = platformUserContext.getCurrentUser();

        final TempEmployer tempEmployer = cloneEmployerToTempEmployer(employer, resolveOldTempEmployer(currentSstInfo));

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
        regCase.setCreatedByUsername(currentUser.username());
        regCase.setCreatedDate(LocalDateTime.now());
        regCase.setDocumentReceivedDate(LocalDateTime.now());

        final RegGeneralInfo savedCase = regGeneralInfoRepository.save(regCase);

        final TempSstInfo tempSstInfo = cloneSstInfoToTempSstInfo(currentSstInfo, savedCase.getTempEmployer().getId());
        final TempSstInfo savedTempSstInfo = tempSstInfoRepository.save(tempSstInfo);

        cloneDirectors(savedCase.getId(), directors);
        clonePremises(savedCase.getId(), premisesList);
        cloneTariffCodes(savedTempSstInfo.getId(), tariffCodes);
        cloneServiceCategories(savedTempSstInfo.getId(), serviceCategories);

        return CommandProcessingResult.resourceResult(savedCase.getId(), savedCase.getCaseRefNo());
    }

    @Transactional(readOnly = true)
    public List<TaxPayerUpdateDiffData> getUpdateDiff(final Long caseId) {
        final RegGeneralInfo regCase = loadCase(caseId);
        if (!RegistrationSectionRouting.isUpdateTaxSection(regCase.getSectionId())) {
            return List.of();
        }
        final Long employerId = regCase.getEmployerId();
        if (employerId == null) {
            return List.of();
        }
        final Optional<Employer> employerOpt = employerRepository.findById(employerId);
        if (employerOpt.isEmpty()) {
            return List.of();
        }
        final Employer employer = employerOpt.get();
        final Optional<SstInfo> currentSstInfoOpt = sstInfoRepository
                .findFirstByEmployerIdAndDeletedFalseOrderByIdDesc(employerId);

        final TempEmployer tempEmployer = regCase.getTempEmployer();
        final TempSstInfo tempSstInfo = tempEmployer != null
                ? tempSstInfoRepository.findByTempEmployerId(tempEmployer.getId()).orElse(null)
                : null;

        final TempEmployer oldAddressSource = resolveOldTempEmployerExcluding(currentSstInfoOpt.orElse(null),
                regCase.getId());

        final List<TaxPayerUpdateDiffData> diffs = new ArrayList<>();
        addIfChanged(diffs, "Employer Name", employer.getEmployerName(),
                tempEmployer != null ? tempEmployer.getEmployerName() : null);
        addIfChanged(diffs, "Registration No", employer.getBusinessInfo().getRegistrationNo(),
                tempEmployer != null ? tempEmployer.getBusinessInfo().getRegistrationNo() : null);
        addIfChanged(diffs, "Business Entity Type", formatId(employer.getBusinessInfo().getBusinessEntityTypeId()),
                tempEmployer != null ? formatId(tempEmployer.getBusinessInfo().getBusinessEntityTypeId()) : null);
        addIfChanged(diffs, "Address Line 1", oldAddressSource != null ? oldAddressSource.getAddressLine1() : null,
                tempEmployer != null ? tempEmployer.getAddressLine1() : null);
        addIfChanged(diffs, "Address Line 2", oldAddressSource != null ? oldAddressSource.getAddressLine2() : null,
                tempEmployer != null ? tempEmployer.getAddressLine2() : null);
        addIfChanged(diffs, "Address Line 3", oldAddressSource != null ? oldAddressSource.getAddressLine3() : null,
                tempEmployer != null ? tempEmployer.getAddressLine3() : null);
        addIfChanged(diffs, "Postcode", oldAddressSource != null ? oldAddressSource.getPostCode() : null,
                tempEmployer != null ? tempEmployer.getPostCode() : null);
        addIfChanged(diffs, "Business Address State", formatId(oldAddressSource != null ? oldAddressSource.getStateId() : null),
                tempEmployer != null ? formatId(tempEmployer.getStateId()) : null);
        addIfChanged(diffs, "Business Address City",
                oldAddressSource != null ? resolveCityLabel(oldAddressSource.getCityName(), oldAddressSource.getCityId()) : null,
                tempEmployer != null ? resolveCityLabel(tempEmployer.getCityName(), tempEmployer.getCityId()) : null);
        // Correspondence address: same "no columns on Employer itself" situation as the business
        // address block above — oldAddressSource is the correct "old" side, not employer.
        addIfChanged(diffs, "Correspondence Address Line 1",
                oldAddressSource != null ? oldAddressSource.getCorrAddressLine1() : null,
                tempEmployer != null ? tempEmployer.getCorrAddressLine1() : null);
        addIfChanged(diffs, "Correspondence Address Line 2",
                oldAddressSource != null ? oldAddressSource.getCorrAddressLine2() : null,
                tempEmployer != null ? tempEmployer.getCorrAddressLine2() : null);
        addIfChanged(diffs, "Correspondence Address Line 3",
                oldAddressSource != null ? oldAddressSource.getCorrAddressLine3() : null,
                tempEmployer != null ? tempEmployer.getCorrAddressLine3() : null);
        addIfChanged(diffs, "Correspondence Postcode",
                oldAddressSource != null ? oldAddressSource.getCorrPostCode() : null,
                tempEmployer != null ? tempEmployer.getCorrPostCode() : null);
        addIfChanged(diffs, "Correspondence State",
                formatId(oldAddressSource != null ? oldAddressSource.getCorrStateId() : null),
                tempEmployer != null ? formatId(tempEmployer.getCorrStateId()) : null);
        addIfChanged(diffs, "Correspondence City",
                oldAddressSource != null
                        ? resolveCityLabel(oldAddressSource.getCorrCityName(), oldAddressSource.getCorrCityId())
                        : null,
                tempEmployer != null
                        ? resolveCityLabel(tempEmployer.getCorrCityName(), tempEmployer.getCorrCityId())
                        : null);
        // Unlike the address/contact fields above, msic_id IS a column on Employer itself (see
        // Employer#msicId / cloneEmployerToTempEmployer, which copies it straight from the live
        // employer rather than from oldAddressSource) — so employer is the correct "old" side here.
        addIfChanged(diffs, "MSIC Code", formatId(employer.getMsicId()),
                tempEmployer != null ? formatId(tempEmployer.getMsicId()) : null);
        addIfChanged(diffs, "Email", employer.getEmail(), tempEmployer != null ? tempEmployer.getEmail() : null);
        // Employer carries no contact_phones/contact_faxes columns of its own (only TempEmployer
        // does, same situation as the address fields above) — use oldAddressSource, not employer,
        // as the "old" side for these two. "Telephone" already captures the primary phone as its
        // first/only entry via formatContactLines, so a separate "Phone" (employer.getPhone()) row
        // would be a duplicate and has been removed.
        addIfChanged(diffs, "Telephone",
                formatContactLines(oldAddressSource != null ? oldAddressSource.getContactPhones() : null),
                formatContactLines(tempEmployer != null ? tempEmployer.getContactPhones() : null));
        addIfChanged(diffs, "Fax",
                formatContactLines(oldAddressSource != null ? oldAddressSource.getContactFaxes() : null),
                formatContactLines(tempEmployer != null ? tempEmployer.getContactFaxes() : null));

        if (currentSstInfoOpt.isPresent()) {
            final SstInfo currentSstInfo = currentSstInfoOpt.get();
            addIfChanged(diffs, "Trade Name", currentSstInfo.getTradeName(),
                    tempSstInfo != null ? tempSstInfo.getTradeName() : null);
            addIfChanged(diffs, "Tourism Tax Reg No", currentSstInfo.getTourTaxRegNo(),
                    tempSstInfo != null ? tempSstInfo.getTourTaxRegNo() : null);
            addIfChanged(diffs, "MOTAC Reg No", currentSstInfo.getMotacRegNo(),
                    tempSstInfo != null ? tempSstInfo.getMotacRegNo() : null);
            addIfChanged(diffs, "Is Labuan", formatYesNo(currentSstInfo.isLabuan()),
                    tempSstInfo != null ? formatYesNo(tempSstInfo.isLabuan()) : null);
            addIfChanged(diffs, "Contact Person", currentSstInfo.getForm1ContactPerson(),
                    tempSstInfo != null ? tempSstInfo.getForm1ContactPerson() : null);
            addIfChanged(diffs, "Website Address", currentSstInfo.getWebsiteAddress(),
                    tempSstInfo != null ? tempSstInfo.getWebsiteAddress() : null);
            addIfChanged(diffs, "Income Tax Ref No", currentSstInfo.getInTaxRefNo(),
                    tempSstInfo != null ? tempSstInfo.getInTaxRefNo() : null);
            addIfChanged(diffs, "Customs Audit Ref No", currentSstInfo.getCusAudRefNo(),
                    tempSstInfo != null ? tempSstInfo.getCusAudRefNo() : null);
            // Previous-registration replacement fields (also user-editable per
            // TempSstInfoWritePlatformService#applySstFields, previously missing from this diff too).
            addIfChanged(diffs, "Previous Registration No", currentSstInfo.getPreRegNo(),
                    tempSstInfo != null ? tempSstInfo.getPreRegNo() : null);
            addIfChanged(diffs, "Previous Registration Name", currentSstInfo.getPreRegName(),
                    tempSstInfo != null ? tempSstInfo.getPreRegName() : null);
            addIfChanged(diffs, "Date Of Replacement", formatDate(currentSstInfo.getDateOfReplacement()),
                    tempSstInfo != null ? formatDate(tempSstInfo.getDateOfReplacement()) : null);
            addIfChanged(diffs, "Business Commencement Date", formatDate(currentSstInfo.getBusinessComDate()),
                    tempSstInfo != null ? formatDate(tempSstInfo.getBusinessComDate()) : null);
            addIfChanged(diffs, "Manufacturing Commencement Date", formatDate(currentSstInfo.getManComDate()),
                    tempSstInfo != null ? formatDate(tempSstInfo.getManComDate()) : null);
            addIfChanged(diffs, "Date Sale/Value Taxable Goods", formatDate(currentSstInfo.getDateSaleValTaxGoods()),
                    tempSstInfo != null ? formatDate(tempSstInfo.getDateSaleValTaxGoods()) : null);
            addIfChanged(diffs, "Financial Year End Month", formatId(currentSstInfo.getFinYrEndMon()),
                    tempSstInfo != null ? formatId(tempSstInfo.getFinYrEndMon()) : null);
            addIfChanged(diffs, "Annual Taxable Sales Value", formatDecimal(currentSstInfo.getAnTotalTaxSalesVal()),
                    tempSstInfo != null ? formatDecimal(tempSstInfo.getAnTotalTaxSalesVal()) : null);
            addIfChanged(diffs, "Local Sales", formatDecimal(currentSstInfo.getLocalSales()),
                    tempSstInfo != null ? formatDecimal(tempSstInfo.getLocalSales()) : null);
            addIfChanged(diffs, "Export Sales", formatDecimal(currentSstInfo.getExportSales()),
                    tempSstInfo != null ? formatDecimal(tempSstInfo.getExportSales()) : null);
            addIfChanged(diffs, "Sales To Designated Area", formatDecimal(currentSstInfo.getSalesToDesignArea()),
                    tempSstInfo != null ? formatDecimal(tempSstInfo.getSalesToDesignArea()) : null);
            addIfChanged(diffs, "Other Sales", formatDecimal(currentSstInfo.getOthersSales()),
                    tempSstInfo != null ? formatDecimal(tempSstInfo.getOthersSales()) : null);
            addIfChanged(diffs, "Sub Contract Work", formatYesNo(currentSstInfo.isSubContractWork()),
                    tempSstInfo != null ? formatYesNo(tempSstInfo.isSubContractWork()) : null);
            addIfChanged(diffs, "Declaration Accepted", formatYesNo(currentSstInfo.isDeclareTrue()),
                    tempSstInfo != null ? formatYesNo(tempSstInfo.isDeclareTrue()) : null);
            addIfChanged(diffs, "Declaration Date", formatDate(currentSstInfo.getDeclareDate()),
                    tempSstInfo != null ? formatDate(tempSstInfo.getDeclareDate()) : null);
            addIfChanged(diffs, "Applicant Name", currentSstInfo.getApplicantName(),
                    tempSstInfo != null ? tempSstInfo.getApplicantName() : null);
            addIfChanged(diffs, "Identity Card", currentSstInfo.getIdentityCard(),
                    tempSstInfo != null ? tempSstInfo.getIdentityCard() : null);
            addIfChanged(diffs, "Designation", currentSstInfo.getDesignation(),
                    tempSstInfo != null ? tempSstInfo.getDesignation() : null);
            addIfChanged(diffs, "Applicant Email", currentSstInfo.getApplicantEmail(),
                    tempSstInfo != null ? tempSstInfo.getApplicantEmail() : null);
            addIfChanged(diffs, "Applicant Telephone", currentSstInfo.getApplicantTelNo(),
                    tempSstInfo != null ? tempSstInfo.getApplicantTelNo() : null);
            // Digital Tax (section 1203) only fields — harmless to compute unconditionally since
            // addIfChanged() already no-ops when both old and new normalize to "" (see below), so
            // these simply never produce a row for the other tax types' update cases.
            addIfChanged(diffs, "Achieving Value Of Digital Service Date",
                    formatDate(currentSstInfo.getAchievingValueOfDsDate()),
                    tempSstInfo != null ? formatDate(tempSstInfo.getAchievingValueOfDsDate()) : null);
            addIfChanged(diffs, "Digital Service Total Value", formatDecimal(currentSstInfo.getDsTotalValue()),
                    tempSstInfo != null ? formatDecimal(tempSstInfo.getDsTotalValue()) : null);
            addIfChanged(diffs, "Digital Service Types", formatDsTypes(currentSstInfo),
                    tempSstInfo != null ? formatDsTypes(tempSstInfo) : null);
        }

        return diffs;
    }

    private Employer loadEmployer(final Long employerId) {
        return employerRepository.findById(employerId)
                .orElseThrow(() -> new ResourceNotFoundException("Employer not found: " + employerId));
    }

    private RegGeneralInfo loadCase(final Long caseId) {
        return regGeneralInfoRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration case not found: " + caseId));
    }

    /**
     * The Employer entity carries no address/postcode columns of its own (only {@code TempEmployer}
     * does) — mirrors {@code TaxPayerSearchReadPlatformService#findRegistrationTempEmployer}: use
     * the temp employer of whichever case last created/touched the live SstInfo record as the best
     * available source for address/contact fields when seeding a fresh update draft.
     */
    private Optional<TempEmployer> resolveOldTempEmployer(final SstInfo currentSstInfo) {
        if (currentSstInfo.getRegGeneralInfoId() == null) {
            return Optional.empty();
        }
        return regGeneralInfoRepository.findById(currentSstInfo.getRegGeneralInfoId()).map(RegGeneralInfo::getTempEmployer);
    }

    /** Same as {@link #resolveOldTempEmployer} but never resolves to the case currently being diffed. */
    private TempEmployer resolveOldTempEmployerExcluding(final SstInfo currentSstInfo, final Long excludeCaseId) {
        if (currentSstInfo == null || currentSstInfo.getRegGeneralInfoId() == null
                || currentSstInfo.getRegGeneralInfoId().equals(excludeCaseId)) {
            return null;
        }
        return regGeneralInfoRepository.findById(currentSstInfo.getRegGeneralInfoId())
                .map(RegGeneralInfo::getTempEmployer).orElse(null);
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

    private static void addIfChanged(final List<TaxPayerUpdateDiffData> diffs, final String fieldLabel,
            final String oldValue, final String newValue) {
        final String oldNormalized = oldValue == null ? "" : oldValue;
        final String newNormalized = newValue == null ? "" : newValue;
        if (!oldNormalized.equals(newNormalized)) {
            diffs.add(TaxPayerUpdateDiffData.builder().fieldLabel(fieldLabel).oldValue(oldNormalized)
                    .newValue(newNormalized).build());
        }
    }

    /**
     * {@code contactPhones}/{@code contactFaxes} are stored as a raw JSON array of
     * {@code {"head": "...", "back": "..."}} pairs (country/area-code prefix + rest of the
     * number), e.g. {@code [{"head":"+60","back":"12345"}]}. Renders them the same way the
     * "Phone" field / the frontend's {@code formatPrimaryContact} does — {@code head + back}
     * with no space — joining multiple entries with ", " instead of dumping the raw JSON.
     */
    private String formatContactLines(final String json) {
        if (json == null || json.isBlank()) {
            return "";
        }
        try {
            final JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isArray()) {
                return "";
            }
            final List<String> lines = new ArrayList<>();
            for (final JsonNode entry : root) {
                final String back = entry.path("back").asText("");
                if (back.isBlank()) {
                    continue;
                }
                final String head = entry.path("head").asText("");
                lines.add((head + back).replaceAll("\\s+", ""));
            }
            return String.join(", ", lines);
        } catch (Exception ex) {
            return "";
        }
    }

    private static String formatDate(final LocalDate date) {
        return date != null ? date.toString() : null;
    }

    private static String formatDecimal(final BigDecimal value) {
        return value != null ? value.toPlainString() : null;
    }

    private static String formatId(final Object id) {
        return id != null ? id.toString() : null;
    }

    /**
     * City is stored as both a lookup id ({@code cityId}) and a free-text human-readable name
     * ({@code cityName}) on {@code TempEmployer} — the write side ({@code applyContactAndAddress}
     * in {@code RegistrationCaseWritePlatformService}) accepts and persists both independently, but
     * {@code cityName} is what's actually shown to users elsewhere, so prefer it when populated and
     * fall back to the raw id (same "just show the id" treatment as the other lookup-id fields in
     * this diff) only when no name was recorded.
     */
    private static String resolveCityLabel(final String cityName, final Long cityId) {
        if (cityName != null && !cityName.isBlank()) {
            return cityName;
        }
        return formatId(cityId);
    }

    private static String formatYesNo(final boolean value) {
        return value ? "Yes" : "No";
    }

    /** Digital Tax "types of digital service" checkboxes, summarized as one comma-joined row. */
    private static String formatDsTypes(final SstInfo sstInfo) {
        return formatDsTypes(sstInfo.isDsTypeSoftwareAppsGame(), sstInfo.isDsTypeMusicEbookFilm(),
                sstInfo.isDsTypeAdOnlinePlatform(), sstInfo.isDsTypeSearchEngineSocialNetwork(),
                sstInfo.isDsTypeDatabaseHosting(), sstInfo.isDsTypeInternetBasedTelecom(),
                sstInfo.isDsTypeOnlineTraining(), sstInfo.isDsTypeOthers());
    }

    private static String formatDsTypes(final TempSstInfo tempSstInfo) {
        return formatDsTypes(tempSstInfo.isDsTypeSoftwareAppsGame(), tempSstInfo.isDsTypeMusicEbookFilm(),
                tempSstInfo.isDsTypeAdOnlinePlatform(), tempSstInfo.isDsTypeSearchEngineSocialNetwork(),
                tempSstInfo.isDsTypeDatabaseHosting(), tempSstInfo.isDsTypeInternetBasedTelecom(),
                tempSstInfo.isDsTypeOnlineTraining(), tempSstInfo.isDsTypeOthers());
    }

    private static String formatDsTypes(final boolean softwareAppsGame, final boolean musicEbookFilm,
            final boolean adOnlinePlatform, final boolean searchEngineSocialNetwork, final boolean databaseHosting,
            final boolean internetBasedTelecom, final boolean onlineTraining, final boolean others) {
        final List<String> labels = new ArrayList<>();
        if (softwareAppsGame) {
            labels.add("Software/Apps/Games");
        }
        if (musicEbookFilm) {
            labels.add("Music/E-book/Film");
        }
        if (adOnlinePlatform) {
            labels.add("Advertising/Online Platform");
        }
        if (searchEngineSocialNetwork) {
            labels.add("Search Engine/Social Network");
        }
        if (databaseHosting) {
            labels.add("Database/Hosting");
        }
        if (internetBasedTelecom) {
            labels.add("Internet-based Telecommunication");
        }
        if (onlineTraining) {
            labels.add("Online Training");
        }
        if (others) {
            labels.add("Others");
        }
        return String.join(", ", labels);
    }
}
