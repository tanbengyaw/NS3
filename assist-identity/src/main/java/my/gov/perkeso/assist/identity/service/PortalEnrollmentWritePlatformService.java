package my.gov.perkeso.assist.identity.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException;
import my.gov.perkeso.assist.core.security.PlatformUser;
import my.gov.perkeso.assist.core.security.PlatformUserContext;
import my.gov.perkeso.assist.identity.constant.PortalApplicationType;
import my.gov.perkeso.assist.identity.constant.PortalEnrollmentStatus;
import my.gov.perkeso.assist.identity.data.PortalEnrollmentRequest;
import my.gov.perkeso.assist.identity.data.PortalUserData;
import my.gov.perkeso.assist.identity.domain.StaffUserRepository;
import my.gov.perkeso.assist.registration.domain.Employer;
import my.gov.perkeso.assist.registration.domain.EmployerRepository;
import my.gov.perkeso.assist.registration.domain.PortalUser;
import my.gov.perkeso.assist.registration.domain.PortalUserRepository;
import my.gov.perkeso.assist.registration.domain.base.BaseReferenceIds;
import my.gov.perkeso.assist.registration.domain.base.UserEmployer;
import my.gov.perkeso.assist.registration.service.BaseUserEmployerEnrollmentCommand;
import my.gov.perkeso.assist.registration.service.BaseUserEmployerEnrollmentService;
import my.gov.perkeso.assist.registration.service.SstNotificationWriteService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortalEnrollmentWritePlatformService {

    private final PortalUserRepository portalUserRepository;
    private final EmployerRepository employerRepository;
    private final StaffUserRepository staffUserRepository;
    private final BaseUserEmployerEnrollmentService baseUserEmployerEnrollmentService;
    private final PlatformUserContext platformUserContext;
    private final PasswordEncoder passwordEncoder;
    private final SstNotificationWriteService sstNotificationWriteService;

    @Transactional
    public PortalUserData enroll(final PortalEnrollmentRequest request) {
        validateEnrollmentRequest(request);
        requireNonBlank(request.getUsername(), "username");

        final String username = request.getUsername().trim();
        final String email = request.getEmail().trim();

        portalUserRepository.findByUsernameIgnoreCase(username).ifPresent(existing -> {
            throw new IllegalArgumentException("Portal username already exists: " + username);
        });
        portalUserRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            throw new IllegalArgumentException("Portal email already exists: " + email);
        });
        assertUsernameAvailableForPortalLogin(username);

        final PortalApplicationType applicationType = PortalApplicationType.fromValue(request.getApplicationType());
        final PortalUser portalUser = buildPortalUser(request, username, email, applicationType);
        final Long registrationEmployerId = resolveRegistrationEmployerId(request, applicationType, portalUser);

        final PortalUser savedPortalUser = portalUserRepository.save(portalUser);
        linkUserEmployer(savedPortalUser, request, applicationType, registrationEmployerId);
        sstNotificationWriteService.notifyPortalUser(savedPortalUser.getId(),
                SstNotificationWriteService.PORTAL_ENROLLMENT_SUBMITTED);

        return PortalUserMapper.toData(savedPortalUser);
    }

    @Transactional
    public PortalUserData queryEnrollment(final String username, final String remark) {
        assertOfficerAction();
        requireNonBlank(username, "username");
        requireNonBlank(remark, "remark");

        final PortalUser portalUser = loadPortalUser(username.trim());
        assertEnrollmentStatus(portalUser, PortalEnrollmentStatus.SUBMITTED);

        portalUser.setEnrollmentStatus(PortalEnrollmentStatus.IN_QUERY.name());
        portalUser.setQueryRemark(remark.trim());
        portalUserRepository.save(portalUser);

        return PortalUserMapper.toData(portalUser);
    }

    @Transactional
    public PortalUserData resubmitEnrollment(final String username, final PortalEnrollmentRequest request) {
        validateEnrollmentRequest(request);
        requireNonBlank(username, "username");

        final PortalUser portalUser = loadPortalUser(username.trim());
        assertEnrollmentStatus(portalUser, PortalEnrollmentStatus.IN_QUERY);

        if (portalUser.getUserEmployerId() == null) {
            throw new IllegalStateException("Portal enrollment profile is incomplete for user: " + username);
        }

        final PortalApplicationType applicationType = PortalApplicationType.fromValue(portalUser.getApplicationType());
        final Long registrationEmployerId = resolveRegistrationEmployerIdForResubmit(request, applicationType, portalUser);

        applyRequestToPortalUser(portalUser, request);
        portalUser.setEnrollmentStatus(PortalEnrollmentStatus.SUBMITTED.name());
        portalUser.setQueryRemark(null);
        portalUserRepository.save(portalUser);

        baseUserEmployerEnrollmentService.updateEnrollmentProfile(portalUser.getUserEmployerId(),
                toEnrollmentCommand(portalUser.getId(), request, applicationType, registrationEmployerId));

        return PortalUserMapper.toData(portalUser);
    }

    @Transactional
    public PortalUserData approveEnrollment(final String username, final String password) {
        assertOfficerAction();
        requireNonBlank(password, "password");

        final PortalUser portalUser = loadPortalUser(username.trim());
        assertEnrollmentStatus(portalUser, PortalEnrollmentStatus.SUBMITTED);
        assertUsernameAvailableForPortalLogin(portalUser.getUsername());

        portalUser.setEnrollmentStatus(PortalEnrollmentStatus.APPROVED.name());
        portalUser.setQueryRemark(null);
        portalUser.setPasswordHash(passwordEncoder.encode(password));
        portalUser.setActive(true);
        portalUser.setApprovedDate(LocalDateTime.now());
        portalUserRepository.save(portalUser);
        sstNotificationWriteService.notifyPortalUser(portalUser.getId(),
                SstNotificationWriteService.PORTAL_ENROLLMENT_APPROVED);

        return PortalUserMapper.toData(portalUser);
    }

    @Transactional
    public PortalUserData rejectEnrollment(final String username) {
        assertOfficerAction();

        final PortalUser portalUser = loadPortalUser(username.trim());
        assertEnrollmentStatus(portalUser, PortalEnrollmentStatus.SUBMITTED);

        portalUser.setEnrollmentStatus(PortalEnrollmentStatus.REJECTED.name());
        portalUser.setActive(false);
        portalUser.setPasswordHash(null);
        portalUserRepository.save(portalUser);

        return PortalUserMapper.toData(portalUser);
    }

    private void linkUserEmployer(final PortalUser savedPortalUser, final PortalEnrollmentRequest request,
            final PortalApplicationType applicationType, final Long registrationEmployerId) {
        final UserEmployer userEmployer = baseUserEmployerEnrollmentService.submitEnrollment(
                toEnrollmentCommand(savedPortalUser.getId(), request, applicationType, registrationEmployerId));
        savedPortalUser.setUserEmployerId(userEmployer.getId());
        portalUserRepository.save(savedPortalUser);
    }

    private void validateEnrollmentRequest(final PortalEnrollmentRequest request) {
        requireNonBlank(request.getEmail(), "email");
        requireNonBlank(request.getEmployerName(), "employerName");
        requireNonNull(request.getRegistrationTypeId(), "registrationTypeId");
        requireNonBlank(request.getRegistrationNo(), "registrationNo");
        requireNonBlank(request.getAddressLine1(), "addressLine1");
        requireNonNull(request.getStateId(), "stateId");
        requireNonBlank(request.getPostCode(), "postCode");
        requireNonBlank(request.getFullName(), "fullName");
        requireNonNull(request.getIdentificationTypeId(), "identificationTypeId");
        requireNonBlank(request.getIdentificationNo(), "identificationNo");
        requireNonBlank(request.getPhoneNumber(), "phoneNumber");
        requireNonBlank(request.getSecurityPhrase(), "securityPhrase");
    }

    private Long resolveRegistrationEmployerId(final PortalEnrollmentRequest request,
            final PortalApplicationType applicationType, final PortalUser portalUser) {
        if (applicationType != PortalApplicationType.EXISTING_EMPLOYER) {
            return null;
        }
        requireNonBlank(request.getEmployerCode(), "employerCode");
        final String employerCode = request.getEmployerCode().trim();
        final Employer employer = employerRepository.findByEmployerCodeAndDeletedFalse(employerCode)
                .orElseThrow(() -> new ResourceNotFoundException("Employer not found: " + employerCode));
        portalUser.setEmployerId(employer.getId());
        portalUser.setEmployerCode(employer.getEmployerCode());
        portalUser.setLinkedDate(LocalDateTime.now());
        return employer.getId();
    }

    private Long resolveRegistrationEmployerIdForResubmit(final PortalEnrollmentRequest request,
            final PortalApplicationType applicationType, final PortalUser portalUser) {
        if (applicationType != PortalApplicationType.EXISTING_EMPLOYER) {
            portalUser.setEmployerId(null);
            portalUser.setEmployerCode(null);
            return null;
        }
        requireNonBlank(request.getEmployerCode(), "employerCode");
        final String employerCode = request.getEmployerCode().trim();
        final Employer employer = employerRepository.findByEmployerCodeAndDeletedFalse(employerCode)
                .orElseThrow(() -> new ResourceNotFoundException("Employer not found: " + employerCode));
        portalUser.setEmployerId(employer.getId());
        portalUser.setEmployerCode(employer.getEmployerCode());
        if (portalUser.getLinkedDate() == null) {
            portalUser.setLinkedDate(LocalDateTime.now());
        }
        return employer.getId();
    }

    private static void applyRequestToPortalUser(final PortalUser portalUser, final PortalEnrollmentRequest request) {
        portalUser.setEmail(request.getEmail().trim());
        portalUser.setEmployerName(trimToNull(request.getEmployerName()));
        portalUser.setRegistrationTypeId(request.getRegistrationTypeId());
        portalUser.setRegistrationNo(trimToNull(request.getRegistrationNo()));
        portalUser.setAddressLine1(trimToNull(request.getAddressLine1()));
        portalUser.setAddressLine2(trimToNull(request.getAddressLine2()));
        portalUser.setAddressLine3(trimToNull(request.getAddressLine3()));
        portalUser.setStateId(request.getStateId());
        portalUser.setCityId(request.getCityId());
        portalUser.setCityName(trimToNull(request.getCityName()));
        portalUser.setPostCode(trimToNull(request.getPostCode()));
        portalUser.setFullName(trimToNull(request.getFullName()));
        portalUser.setIdentificationTypeId(request.getIdentificationTypeId());
        portalUser.setIdentificationNo(trimToNull(request.getIdentificationNo()));
        portalUser.setPhoneCallingCode(defaultCallingCode(request.getPhoneCallingCode()));
        portalUser.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        portalUser.setSecurityPhrase(trimToNull(request.getSecurityPhrase()));
    }

    private BaseUserEmployerEnrollmentCommand toEnrollmentCommand(final Long portalUserId,
            final PortalEnrollmentRequest request, final PortalApplicationType applicationType,
            final Long registrationEmployerId) {
        return new BaseUserEmployerEnrollmentCommand(
                portalUserId,
                toLegacyApplicationType(applicationType),
                trimToNull(request.getEmployerCode()),
                trimToNull(request.getEmployerName()),
                request.getRegistrationTypeId(),
                trimToNull(request.getRegistrationNo()),
                trimToNull(request.getAddressLine1()),
                trimToNull(request.getAddressLine2()),
                trimToNull(request.getAddressLine3()),
                request.getStateId(),
                request.getCityId(),
                trimToNull(request.getPostCode()),
                trimToNull(request.getFullName()),
                request.getIdentificationTypeId(),
                trimToNull(request.getIdentificationNo()),
                defaultCallingCode(request.getPhoneCallingCode()),
                trimToNull(request.getPhoneNumber()),
                request.getEmail().trim(),
                trimToNull(request.getSecurityPhrase()),
                registrationEmployerId,
                trimToNull(request.getDraftToken()));
    }

    private PortalUser loadPortalUser(final String username) {
        return portalUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Portal user not found: " + username));
    }

    private static void assertEnrollmentStatus(final PortalUser portalUser,
            final PortalEnrollmentStatus expectedStatus) {
        final PortalEnrollmentStatus currentStatus = PortalEnrollmentStatus.fromValue(portalUser.getEnrollmentStatus());
        if (currentStatus != expectedStatus) {
            throw new IllegalArgumentException(
                    "Portal enrollment must be " + expectedStatus.name() + " but was " + currentStatus.name());
        }
    }

    private void assertOfficerAction() {
        final PlatformUser currentUser = platformUserContext.getCurrentUser();
        if (currentUser.isEmployer()) {
            throw new IllegalArgumentException("Only staff can manage portal enrollments");
        }
    }

    private void assertUsernameAvailableForPortalLogin(final String username) {
        staffUserRepository.findByUsernameIgnoreCase(username).ifPresent(existing -> {
            throw new IllegalArgumentException("Username already used by a staff account: " + username);
        });
    }

    private static PortalUser buildPortalUser(final PortalEnrollmentRequest request, final String username,
            final String email, final PortalApplicationType applicationType) {
        final PortalUser portalUser = new PortalUser();
        portalUser.setUsername(username);
        portalUser.setEmail(email);
        portalUser.setApplicationType(applicationType.name());
        portalUser.setEnrolledDate(LocalDateTime.now());
        portalUser.setEnrollmentStatus(PortalEnrollmentStatus.SUBMITTED.name());
        applyRequestToPortalUser(portalUser, request);
        return portalUser;
    }

    private static int toLegacyApplicationType(final PortalApplicationType applicationType) {
        return applicationType == PortalApplicationType.NEW_EMPLOYER
                ? BaseReferenceIds.APPLICATION_TYPE_NEW_EMPLOYER
                : BaseReferenceIds.APPLICATION_TYPE_EXISTING_EMPLOYER;
    }

    private static String defaultCallingCode(final String value) {
        if (value == null || value.isBlank()) {
            return "+60";
        }
        return value.trim();
    }

    private static String trimToNull(final String value) {
        if (value == null) {
            return null;
        }
        final String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
