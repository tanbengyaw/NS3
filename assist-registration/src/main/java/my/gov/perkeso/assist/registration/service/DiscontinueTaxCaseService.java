package my.gov.perkeso.assist.registration.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.infrastructure.data.CommandProcessingResult;
import my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException;
import my.gov.perkeso.assist.core.security.PlatformUser;
import my.gov.perkeso.assist.core.security.PlatformUserContext;
import my.gov.perkeso.assist.registration.constant.DataSource;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import my.gov.perkeso.assist.registration.constant.RegistrationSectionRouting;
import my.gov.perkeso.assist.registration.constant.SstStatus;
import my.gov.perkeso.assist.registration.data.DiscontinueTaxInfoData;
import my.gov.perkeso.assist.registration.domain.AppStatus;
import my.gov.perkeso.assist.registration.domain.BusinessInfo;
import my.gov.perkeso.assist.registration.domain.Employer;
import my.gov.perkeso.assist.registration.domain.EmployerRepository;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfoRepository;
import my.gov.perkeso.assist.registration.domain.SstInfo;
import my.gov.perkeso.assist.registration.domain.SstInfoRepository;
import my.gov.perkeso.assist.registration.domain.SstStatusInfo;
import my.gov.perkeso.assist.registration.domain.SstStatusInfoRepository;
import my.gov.perkeso.assist.registration.domain.TempDiscontinueTax;
import my.gov.perkeso.assist.registration.domain.TempDiscontinueTaxRepository;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import my.gov.perkeso.assist.registration.exception.RegistrationCaseInvalidStatusException;
import my.gov.perkeso.assist.registration.reference.ReferenceNoGeneratorFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Starts and promotes "Discontinue Tax" cases (section 1103) — mirrors ASSIST
 * {@code DiscontinueTaxFormDto} / {@code SstStatusEnum}: change a taxpayer's active tax status to
 * cancelled as of an effective date. Unlike Update Tax Payer, no employer/SstInfo field is ever
 * edited — only the target {@code SstStatusInfo} row is flipped on approval.
 */
@Service
@RequiredArgsConstructor
public class DiscontinueTaxCaseService {

    private final EmployerRepository employerRepository;
    private final SstInfoRepository sstInfoRepository;
    private final SstStatusInfoRepository sstStatusInfoRepository;
    private final RegGeneralInfoRepository regGeneralInfoRepository;
    private final TempDiscontinueTaxRepository tempDiscontinueTaxRepository;
    private final TempSstInfoWritePlatformService tempSstInfoWritePlatformService;
    private final ReferenceNoGeneratorFactory referenceNoGeneratorFactory;
    private final PlatformUserContext platformUserContext;

    @Transactional
    public CommandProcessingResult startDiscontinue(final Long employerId, final Long sstInfoId) {
        if (employerId == null) {
            throw new IllegalArgumentException("employerId is required");
        }
        if (sstInfoId == null) {
            throw new IllegalArgumentException("sstInfoId is required");
        }

        final Employer employer = loadEmployer(employerId);
        final SstInfo sstInfo = sstInfoRepository.findById(sstInfoId)
                .orElseThrow(() -> new ResourceNotFoundException("SST info not found: " + sstInfoId));
        if (!employer.getId().equals(sstInfo.getEmployerId())) {
            throw new IllegalArgumentException("SST info " + sstInfoId + " does not belong to employer " + employerId);
        }
        final SstStatusInfo currentStatus = currentStatusFor(sstInfoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "SST info " + sstInfoId + " has no current active status"));

        final RegistrationSection section = RegistrationSection.REG_SST_DISCONTINUE_TAX;
        final PlatformUser currentUser = platformUserContext.getCurrentUser();

        final RegGeneralInfo regCase = new RegGeneralInfo();
        regCase.setCaseRefNo(referenceNoGeneratorFactory.generateForSection(section));
        regCase.setAppStatus(AppStatus.NEW);
        regCase.setRegType(section.name());
        regCase.setSectionId(section.getAssistSectionId());
        regCase.setDataSourceId(DataSource.OTC.getAssistId());
        regCase.setPksBranchId(employer.getPksBranchId());
        regCase.setProcessingPksBranchId(employer.getPksBranchId());
        regCase.setReceivingPksBranchId(employer.getPksBranchId());
        regCase.setTempEmployer(cloneEmployerToTempEmployer(employer, resolveOldTempEmployer(sstInfo)));
        regCase.setEmployerId(employer.getId());
        regCase.setCreatedByUsername(currentUser.username());
        regCase.setCreatedDate(LocalDateTime.now());
        regCase.setDocumentReceivedDate(LocalDateTime.now());

        final RegGeneralInfo savedCase = regGeneralInfoRepository.save(regCase);

        // Supporting-document upload reuses the existing SST draft APIs, which require a
        // TempSstInfo row to attach to — an empty one is enough since no SstInfo fields are
        // editable for this section.
        tempSstInfoWritePlatformService.requireOrCreateTempSstInfo(savedCase);

        final TempDiscontinueTax draft = new TempDiscontinueTax();
        draft.setRegGeneralInfoId(savedCase.getId());
        draft.setSstInfoId(sstInfoId);
        draft.setTaxTypeId(currentStatus.getTaxTypeId());
        draft.setCreatedDate(LocalDateTime.now());
        tempDiscontinueTaxRepository.save(draft);

        return CommandProcessingResult.resourceResult(savedCase.getId(), savedCase.getCaseRefNo());
    }

    @Transactional(readOnly = true)
    public DiscontinueTaxInfoData getDiscontinueInfo(final Long caseId) {
        final RegGeneralInfo regCase = loadCase(caseId);
        final TempDiscontinueTax draft = requireDraft(caseId);
        final SstStatusInfo currentStatus = currentStatusFor(draft.getSstInfoId(), draft.getTaxTypeId())
                .orElse(null);

        return DiscontinueTaxInfoData.builder()
                .caseId(regCase.getId())
                .sstInfoId(draft.getSstInfoId())
                .taxTypeId(draft.getTaxTypeId())
                .taxTypeLabel(taxTypeLabel(draft.getTaxTypeId()))
                .currentSstStatusId(currentStatus != null ? currentStatus.getSstStatusId() : null)
                .currentSstStatusLabel(currentStatus != null ? statusLabel(currentStatus.getSstStatusId()) : null)
                .newSstStatusId(draft.getNewSstStatusId())
                .cessationTaxEffectiveFrom(draft.getCessationTaxEffectiveFrom())
                .build();
    }

    @Transactional
    public DiscontinueTaxInfoData upsertDiscontinueInfo(final Long caseId, final Long newSstStatusId,
            final LocalDate cessationTaxEffectiveFrom) {
        final RegGeneralInfo regCase = loadEditableCase(caseId);
        final TempDiscontinueTax draft = requireDraft(caseId);
        draft.setNewSstStatusId(newSstStatusId);
        draft.setCessationTaxEffectiveFrom(cessationTaxEffectiveFrom);
        draft.setUpdatedDate(LocalDateTime.now());
        tempDiscontinueTaxRepository.save(draft);
        return getDiscontinueInfo(regCase.getId());
    }

    /**
     * Approval promotion: flips the target {@code SstStatusInfo} row (current -> not current, end
     * date set) and inserts a fresh one with the requested new status. Returns a small summary map
     * for the case's {@code changes} response.
     */
    @Transactional
    public Map<String, Object> promoteOnApprove(final RegGeneralInfo regCase) {
        final TempDiscontinueTax draft = requireDraft(regCase.getId());
        if (draft.getNewSstStatusId() == null) {
            throw new IllegalArgumentException("Discontinue tax case has no requested new status: " + regCase.getId());
        }
        final SstStatusInfo currentStatus = currentStatusFor(draft.getSstInfoId(), draft.getTaxTypeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No current status found for SST info " + draft.getSstInfoId()));

        currentStatus.setCurrent(false);
        currentStatus.setEndDate(draft.getCessationTaxEffectiveFrom() != null
                ? draft.getCessationTaxEffectiveFrom().minusDays(1)
                : LocalDate.now());
        sstStatusInfoRepository.save(currentStatus);

        final SstStatusInfo newStatus = new SstStatusInfo();
        newStatus.setSstInfoId(draft.getSstInfoId());
        newStatus.setTaxTypeId(draft.getTaxTypeId());
        newStatus.setSstStatusId(draft.getNewSstStatusId());
        newStatus.setStartDate(draft.getCessationTaxEffectiveFrom() != null
                ? draft.getCessationTaxEffectiveFrom()
                : LocalDate.now());
        newStatus.setCurrent(true);
        newStatus.setDeleted(false);
        newStatus.setCreatedDate(LocalDateTime.now());
        sstStatusInfoRepository.save(newStatus);

        return Map.of(
                "sstInfoId", draft.getSstInfoId(),
                "taxType", taxTypeLabel(draft.getTaxTypeId()),
                "newStatus", statusLabel(draft.getNewSstStatusId()),
                "cessationTaxEffectiveFrom",
                draft.getCessationTaxEffectiveFrom() != null ? draft.getCessationTaxEffectiveFrom().toString() : "");
    }

    private Optional<SstStatusInfo> currentStatusFor(final Long sstInfoId) {
        return sstStatusInfoRepository.findBySstInfoIdAndDeletedFalse(sstInfoId).stream()
                .filter(SstStatusInfo::isCurrent)
                .findFirst();
    }

    private Optional<SstStatusInfo> currentStatusFor(final Long sstInfoId, final Long taxTypeId) {
        return sstStatusInfoRepository.findBySstInfoIdAndDeletedFalse(sstInfoId).stream()
                .filter(SstStatusInfo::isCurrent)
                .filter(status -> taxTypeId == null || taxTypeId.equals(status.getTaxTypeId()))
                .findFirst();
    }

    private TempDiscontinueTax requireDraft(final Long caseId) {
        return tempDiscontinueTaxRepository.findByRegGeneralInfoId(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Discontinue tax draft not found for case: " + caseId));
    }

    private Employer loadEmployer(final Long employerId) {
        return employerRepository.findById(employerId)
                .orElseThrow(() -> new ResourceNotFoundException("Employer not found: " + employerId));
    }

    private RegGeneralInfo loadCase(final Long caseId) {
        return regGeneralInfoRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration case not found: " + caseId));
    }

    private RegGeneralInfo loadEditableCase(final Long caseId) {
        final RegGeneralInfo regCase = loadCase(caseId);
        if (regCase.getAppStatus() != AppStatus.NEW && regCase.getAppStatus() != AppStatus.IN_QUERY
                && regCase.getAppStatus() != AppStatus.IN_PROGRESS) {
            throw new RegistrationCaseInvalidStatusException(regCase.getCaseRefNo(), regCase.getAppStatus().name(),
                    AppStatus.NEW.name());
        }
        return regCase;
    }

    /** Best-effort address source for display, same rationale as TaxPayerUpdateCaseService. */
    private Optional<TempEmployer> resolveOldTempEmployer(final SstInfo sstInfo) {
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
        });

        return tempEmployer;
    }

    private static String taxTypeLabel(final Long taxTypeId) {
        if (taxTypeId == null) {
            return null;
        }
        for (final my.gov.perkeso.assist.registration.constant.TaxType taxType
                : my.gov.perkeso.assist.registration.constant.TaxType.values()) {
            if (taxType.getAssistId() == taxTypeId) {
                return taxType.name();
            }
        }
        return "TAX_TYPE_" + taxTypeId;
    }

    private static String statusLabel(final Long sstStatusId) {
        if (sstStatusId == null) {
            return null;
        }
        if (sstStatusId.equals(SstStatus.ACTIVE.getAssistId())) {
            return "Active";
        }
        if (sstStatusId.equals(SstStatus.CANCEL.getAssistId())) {
            return "Cancelled";
        }
        return "STATUS_" + sstStatusId;
    }
}
