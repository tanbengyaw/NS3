package my.gov.perkeso.assist.registration.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.commands.api.JsonCommand;
import my.gov.perkeso.assist.core.infrastructure.data.CommandProcessingResult;
import my.gov.perkeso.assist.core.security.PlatformUser;
import my.gov.perkeso.assist.core.security.PlatformUserContext;
import my.gov.perkeso.assist.registration.domain.AppStatus;
import my.gov.perkeso.assist.registration.domain.BusinessInfo;
import my.gov.perkeso.assist.registration.domain.Employer;
import my.gov.perkeso.assist.registration.domain.EmployerRepository;
import my.gov.perkeso.assist.registration.domain.EmployerStatusInfoRepository;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfoRepository;
import my.gov.perkeso.assist.registration.domain.SstInfo;
import my.gov.perkeso.assist.registration.data.RegistrationSpecialCase;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import my.gov.perkeso.assist.registration.exception.RegistrationBrnDuplicateException;
import my.gov.perkeso.assist.registration.exception.RegistrationCaseInvalidStatusException;
import my.gov.perkeso.assist.registration.reference.ReferenceNoGeneratorFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationCaseWritePlatformService {

    private final RegGeneralInfoRepository regGeneralInfoRepository;
    private final EmployerRepository employerRepository;
    private final EmployerStatusInfoRepository employerStatusInfoRepository;
    private final EmployerCodeGenerator employerCodeGenerator;
    private final ReferenceNoGeneratorFactory referenceNoGeneratorFactory;
    private final RegistrationCaseContextService registrationCaseContextService;
    private final RegistrationCaseSubmitValidator registrationCaseSubmitValidator;
    private final RegistrationCaseSubmitRouter registrationCaseSubmitRouter;
    private final PortalUserLinkService portalUserLinkService;
    private final EmployeePromotionService employeePromotionService;
    private final RegistrationSpecialCaseService registrationSpecialCaseService;
    private final SstInfoPromotionService sstInfoPromotionService;
    private final PlatformUserContext platformUserContext;

    @Transactional
    public CommandProcessingResult createCase(final JsonCommand command) {
        validateRequiredFields(command);
        final PlatformUser currentUser = platformUserContext.getCurrentUser();
        if (currentUser.isEmployer()) {
            employerCodeGenerator.assertRegistrationNoUnique(command.stringValueOfParameterNamed("registrationNo"));
        }

        final RegistrationCaseContext caseContext = registrationCaseContextService.resolveForCreate(command,
                currentUser);

        final BusinessInfo businessInfo = new BusinessInfo();
        businessInfo.setRegistrationNo(command.stringValueOfParameterNamed("registrationNo"));
        businessInfo.setBusinessEntityTypeId(command.longValueOfParameterNamed("businessEntityTypeId"));

        final TempEmployer tempEmployer = mapTempEmployer(command, businessInfo);

        final RegGeneralInfo regCase = new RegGeneralInfo();
        regCase.setCaseRefNo(referenceNoGeneratorFactory.generateForSection(caseContext.getSection()));
        regCase.setAppStatus(AppStatus.NEW);
        regCase.setRegType(caseContext.getSection().name());
        regCase.setSectionId(caseContext.getSection().getAssistSectionId());
        regCase.setDataSourceId(caseContext.getDataSource().getAssistId());
        regCase.setPksBranchId(caseContext.getPksBranchId());
        regCase.setProcessingPksBranchId(caseContext.getProcessingPksBranchId());
        regCase.setReceivingPksBranchId(caseContext.getReceivingPksBranchId());
        regCase.setTempEmployer(tempEmployer);
        regCase.setCreatedByUsername(currentUser.username());
        regCase.setCreatedDate(LocalDateTime.now());
        regCase.setDocumentReceivedDate(resolveDocumentReceivedDate(command));

        final RegGeneralInfo saved = regGeneralInfoRepository.save(regCase);
        return CommandProcessingResult.resourceResult(saved.getId(), saved.getCaseRefNo());
    }

    @Transactional
    public CommandProcessingResult updateCase(final JsonCommand command) {
        final RegGeneralInfo regCase = loadCase(command.getEntityId());
        assertStatus(regCase, AppStatus.NEW, AppStatus.IN_QUERY, AppStatus.IN_PROGRESS);

        applyUpdates(regCase.getTempEmployer(), command);
        if (command.stringValueOfParameterNamed("documentReceivedDate") != null) {
            regCase.setDocumentReceivedDate(parseDateTime(command.stringValueOfParameterNamed("documentReceivedDate")));
        }
        regCase.setUpdatedDate(LocalDateTime.now());
        regGeneralInfoRepository.save(regCase);
        return CommandProcessingResult.withChanges(regCase.getId(), regCase.getCaseRefNo(), Map.of("updated", true));
    }

    @Transactional
    public CommandProcessingResult submitCase(final JsonCommand command) {
        final RegGeneralInfo regCase = loadCase(command.getEntityId());
        assertStatus(regCase, AppStatus.NEW, AppStatus.IN_QUERY, AppStatus.IN_PROGRESS);

        registrationCaseSubmitValidator.validateForSubmit(regCase);

        final PlatformUser currentUser = platformUserContext.getCurrentUser();
        final List<RegistrationSpecialCase> specialCases = registrationSpecialCaseService.checkOnSubmit(regCase);
        final AppStatus submitStatus = registrationCaseSubmitRouter.resolveSubmitStatus(currentUser, command,
                specialCases);
        regCase.setIncomplete(registrationCaseSubmitRouter.isIncompleteSubmit(command));
        regCase.setSubmissionDate(LocalDateTime.now());
        regCase.setSubmittedByUsername(currentUser.username());
        regCase.setUpdatedDate(LocalDateTime.now());

        if (submitStatus == AppStatus.APPROVED) {
            if (registrationCaseSubmitRouter.shouldRejectRoDuplicateBrn(currentUser, specialCases)) {
                regCase.setAppStatus(AppStatus.REJECTED);
                regCase.setAppStatusReason(registrationCaseSubmitRouter.duplicateBrnRejectReason(specialCases));
                regGeneralInfoRepository.save(regCase);
                return CommandProcessingResult.withChanges(regCase.getId(), regCase.getCaseRefNo(),
                        submitChanges(AppStatus.REJECTED, specialCases));
            }
            return finalizeRoAutoApprove(regCase);
        }

        regCase.setAppStatus(submitStatus);
        regGeneralInfoRepository.save(regCase);
        return CommandProcessingResult.withChanges(regCase.getId(), regCase.getCaseRefNo(),
                submitChanges(submitStatus, specialCases));
    }

    private static Map<String, Object> submitChanges(final AppStatus appStatus,
            final List<RegistrationSpecialCase> specialCases) {
        final Map<String, Object> changes = new java.util.HashMap<>();
        changes.put("appStatus", appStatus.name());
        if (!specialCases.isEmpty()) {
            changes.put("specialCases", specialCases.stream().map(RegistrationSpecialCase::getType)
                    .map(Enum::name).toList());
        }
        return changes;
    }

    private CommandProcessingResult finalizeRoAutoApprove(final RegGeneralInfo regCase) {
        try {
            return approveAndPromote(regCase);
        } catch (RegistrationBrnDuplicateException ex) {
            regCase.setAppStatus(AppStatus.REJECTED);
            regCase.setAppStatusReason(ex.getMessage());
            regGeneralInfoRepository.save(regCase);
            return CommandProcessingResult.withChanges(regCase.getId(), regCase.getCaseRefNo(),
                    Map.of("appStatus", AppStatus.REJECTED.name(), "reason", ex.getMessage()));
        }
    }

    @Transactional
    public CommandProcessingResult approveCase(final JsonCommand command) {
        final RegGeneralInfo regCase = loadCase(command.getEntityId());
        assertStatus(regCase, AppStatus.SUBMITTED);
        return approveAndPromote(regCase);
    }

    private CommandProcessingResult approveAndPromote(final RegGeneralInfo regCase) {
        final RegistrationSection section = regCase.getSectionId() == null
                ? RegistrationSection.REG_NEW_REG
                : RegistrationSection.fromAssistSectionId(regCase.getSectionId());
        final Employer employer;
        final boolean isNewEmployer;

        if (section == RegistrationSection.REG_NEW_REG_SST_SALES_TAX) {
            final Optional<Employer> existing = employerCodeGenerator.findExistingEmployerByRegistrationNo(
                    regCase.getTempEmployer().getBusinessInfo().getRegistrationNo());
            isNewEmployer = existing.isEmpty();
            employer = isNewEmployer
                    ? employerRepository.save(employerCodeGenerator.promoteOrReuseForSstSales(regCase))
                    : existing.get();
        } else {
            isNewEmployer = true;
            employer = employerRepository.save(
                    employerCodeGenerator.promoteFromTempEmployer(regCase.getTempEmployer(), regCase));
        }

        if (isNewEmployer) {
            employerStatusInfoRepository.save(employerCodeGenerator.createActiveStatus(employer.getId()));
            portalUserLinkService.linkEmployerOnApprove(regCase, employer);
            employeePromotionService.promoteOnApprove(regCase.getTempEmployer().getId(), employer);
        }

        final SstInfo sstInfo = sstInfoPromotionService.promoteSalesTaxOnApprove(regCase, employer);

        regCase.setAppStatus(AppStatus.APPROVED);
        regCase.setEmployerId(employer.getId());
        regCase.setUpdatedDate(LocalDateTime.now());
        regGeneralInfoRepository.save(regCase);

        final Map<String, Object> changes = new java.util.HashMap<>(Map.of("caseRefNo", regCase.getCaseRefNo(),
                "employerCode", employer.getEmployerCode(), "appStatus", AppStatus.APPROVED.name()));
        if (sstInfo != null) {
            changes.put("salesTaxSmkRegNo", sstInfo.getSalesTaxSmkRegNo());
            changes.put("sstInfoId", sstInfo.getId());
        }
        return CommandProcessingResult.withChanges(employer.getId(), employer.getEmployerCode(), changes);
    }

    @Transactional
    public CommandProcessingResult rejectCase(final JsonCommand command) {
        final RegGeneralInfo regCase = loadCase(command.getEntityId());
        assertStatus(regCase, AppStatus.SUBMITTED);
        regCase.setAppStatus(AppStatus.REJECTED);
        regCase.setAppStatusReason(command.stringValueOfParameterNamed("reason"));
        regCase.setUpdatedDate(LocalDateTime.now());
        regGeneralInfoRepository.save(regCase);
        return CommandProcessingResult.resourceResult(regCase.getId(), regCase.getCaseRefNo());
    }

    @Transactional
    public CommandProcessingResult queryCase(final JsonCommand command) {
        final PlatformUser currentUser = platformUserContext.getCurrentUser();
        if (currentUser.isEmployer()) {
            throw new IllegalArgumentException("Only officers can send a registration case to query");
        }

        final RegGeneralInfo regCase = loadCase(command.getEntityId());
        assertStatus(regCase, AppStatus.SUBMITTED);

        final String remark = command.stringValueOfParameterNamed("remark");
        if (remark == null || remark.isBlank()) {
            throw new IllegalArgumentException("remark is required when sending a case to query");
        }

        regCase.setAppStatus(AppStatus.IN_QUERY);
        regCase.setQueryRemark(remark.trim());
        regCase.setInqueryByUsername(currentUser.username());
        regCase.setInqueryDate(LocalDateTime.now());
        regCase.setUpdatedDate(LocalDateTime.now());
        regGeneralInfoRepository.save(regCase);

        return CommandProcessingResult.withChanges(regCase.getId(), regCase.getCaseRefNo(),
                Map.of("appStatus", AppStatus.IN_QUERY.name(), "queryRemark", remark.trim()));
    }

    private RegGeneralInfo loadCase(final Long caseId) {
        return regGeneralInfoRepository.findById(caseId)
                .orElseThrow(() -> new my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException(
                        "Registration case not found: " + caseId));
    }

    private void assertStatus(final RegGeneralInfo regCase, final AppStatus... allowed) {
        for (final AppStatus status : allowed) {
            if (regCase.getAppStatus() == status) {
                return;
            }
        }
        throw new RegistrationCaseInvalidStatusException(regCase.getCaseRefNo(), regCase.getAppStatus().name(),
                allowed[0].name());
    }

    private void validateRequiredFields(final JsonCommand command) {
        requireNonBlank(command.stringValueOfParameterNamed("employerName"), "employerName");
        requireNonBlank(command.stringValueOfParameterNamed("registrationNo"), "registrationNo");
        requireNonNull(command.longValueOfParameterNamed("serviceTypeId"), "serviceTypeId");
        requireNonNull(command.longValueOfParameterNamed("pksBranchId"), "pksBranchId");
        requireNonBlank(command.stringValueOfParameterNamed("postCode"), "postCode");
    }

    private TempEmployer mapTempEmployer(final JsonCommand command, final BusinessInfo businessInfo) {
        final TempEmployer tempEmployer = new TempEmployer();
        tempEmployer.setEmployerName(command.stringValueOfParameterNamed("employerName"));
        tempEmployer.setBusinessInfo(businessInfo);
        tempEmployer.setServiceTypeId(command.longValueOfParameterNamed("serviceTypeId"));
        tempEmployer.setPksBranchId(command.longValueOfParameterNamed("pksBranchId"));
        tempEmployer.setPostCode(command.stringValueOfParameterNamed("postCode"));
        tempEmployer.setBranch(Boolean.TRUE.equals(command.booleanValueOfParameterNamed("isBranch")));
        tempEmployer.setMsicId(command.longValueOfParameterNamed("msicId"));
        tempEmployer.setMethodContributionPaymentId(command.longValueOfParameterNamed("methodContributionPaymentId"));
        tempEmployer.setEmployerRegisterStatus(
                command.longValueOfParameterNamed("employerRegisterStatus") != null
                        ? command.longValueOfParameterNamed("employerRegisterStatus").intValue()
                        : null);
        applyContactAndAddress(tempEmployer, command);
        return tempEmployer;
    }

    private void applyUpdates(final TempEmployer tempEmployer, final JsonCommand command) {
        if (command.stringValueOfParameterNamed("employerName") != null) {
            tempEmployer.setEmployerName(command.stringValueOfParameterNamed("employerName"));
        }
        if (command.stringValueOfParameterNamed("registrationNo") != null) {
            tempEmployer.getBusinessInfo().setRegistrationNo(command.stringValueOfParameterNamed("registrationNo"));
        }
        if (command.longValueOfParameterNamed("businessEntityTypeId") != null) {
            tempEmployer.getBusinessInfo()
                    .setBusinessEntityTypeId(command.longValueOfParameterNamed("businessEntityTypeId"));
        }
        if (command.longValueOfParameterNamed("serviceTypeId") != null) {
            tempEmployer.setServiceTypeId(command.longValueOfParameterNamed("serviceTypeId"));
        }
        if (command.longValueOfParameterNamed("pksBranchId") != null) {
            tempEmployer.setPksBranchId(command.longValueOfParameterNamed("pksBranchId"));
        }
        if (command.stringValueOfParameterNamed("postCode") != null) {
            tempEmployer.setPostCode(command.stringValueOfParameterNamed("postCode"));
        }
        if (command.booleanValueOfParameterNamed("isBranch") != null) {
            tempEmployer.setBranch(command.booleanValueOfParameterNamed("isBranch"));
        }
        if (command.longValueOfParameterNamed("msicId") != null) {
            tempEmployer.setMsicId(command.longValueOfParameterNamed("msicId"));
        }
        if (command.longValueOfParameterNamed("methodContributionPaymentId") != null) {
            tempEmployer.setMethodContributionPaymentId(
                    command.longValueOfParameterNamed("methodContributionPaymentId"));
        }
        applyContactAndAddress(tempEmployer, command);
    }

    private static void applyContactAndAddress(final TempEmployer tempEmployer, final JsonCommand command) {
        if (command.stringValueOfParameterNamed("email") != null) {
            tempEmployer.setEmail(command.stringValueOfParameterNamed("email"));
        }
        if (command.stringValueOfParameterNamed("phone") != null) {
            tempEmployer.setPhone(command.stringValueOfParameterNamed("phone"));
        }
        if (command.stringValueOfParameterNamed("contactPhones") != null) {
            tempEmployer.setContactPhones(command.stringValueOfParameterNamed("contactPhones"));
        }
        if (command.stringValueOfParameterNamed("contactFaxes") != null) {
            tempEmployer.setContactFaxes(command.stringValueOfParameterNamed("contactFaxes"));
        }
        if (command.stringValueOfParameterNamed("addressLine1") != null) {
            tempEmployer.setAddressLine1(command.stringValueOfParameterNamed("addressLine1"));
        }
        if (command.stringValueOfParameterNamed("addressLine2") != null) {
            tempEmployer.setAddressLine2(command.stringValueOfParameterNamed("addressLine2"));
        }
        if (command.stringValueOfParameterNamed("addressLine3") != null) {
            tempEmployer.setAddressLine3(command.stringValueOfParameterNamed("addressLine3"));
        }
        if (command.longValueOfParameterNamed("stateId") != null) {
            tempEmployer.setStateId(command.longValueOfParameterNamed("stateId"));
        }
        if (command.longValueOfParameterNamed("cityId") != null) {
            tempEmployer.setCityId(command.longValueOfParameterNamed("cityId"));
        }
        if (command.stringValueOfParameterNamed("cityName") != null) {
            tempEmployer.setCityName(command.stringValueOfParameterNamed("cityName"));
        }
        applyCorrespondenceAddress(tempEmployer, command);
    }

    private static void applyCorrespondenceAddress(final TempEmployer tempEmployer, final JsonCommand command) {
        if (command.stringValueOfParameterNamed("corrAddressLine1") != null) {
            tempEmployer.setCorrAddressLine1(command.stringValueOfParameterNamed("corrAddressLine1"));
        }
        if (command.stringValueOfParameterNamed("corrAddressLine2") != null) {
            tempEmployer.setCorrAddressLine2(command.stringValueOfParameterNamed("corrAddressLine2"));
        }
        if (command.stringValueOfParameterNamed("corrAddressLine3") != null) {
            tempEmployer.setCorrAddressLine3(command.stringValueOfParameterNamed("corrAddressLine3"));
        }
        if (command.stringValueOfParameterNamed("corrPostCode") != null) {
            tempEmployer.setCorrPostCode(command.stringValueOfParameterNamed("corrPostCode"));
        }
        if (command.longValueOfParameterNamed("corrStateId") != null) {
            tempEmployer.setCorrStateId(command.longValueOfParameterNamed("corrStateId"));
        }
        if (command.longValueOfParameterNamed("corrCityId") != null) {
            tempEmployer.setCorrCityId(command.longValueOfParameterNamed("corrCityId"));
        }
        if (command.stringValueOfParameterNamed("corrCityName") != null) {
            tempEmployer.setCorrCityName(command.stringValueOfParameterNamed("corrCityName"));
        }
    }

    private static LocalDateTime resolveDocumentReceivedDate(final JsonCommand command) {
        final String value = command.stringValueOfParameterNamed("documentReceivedDate");
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }
        return parseDateTime(value);
    }

    private static LocalDateTime parseDateTime(final String value) {
        return LocalDateTime.parse(value);
    }

    private static void requireNonBlank(final String value, final String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private static void requireNonNull(final Object value, final String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
