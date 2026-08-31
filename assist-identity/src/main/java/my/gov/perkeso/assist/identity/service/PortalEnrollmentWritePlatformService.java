package my.gov.perkeso.assist.identity.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException;
import my.gov.perkeso.assist.identity.constant.PortalApplicationType;
import my.gov.perkeso.assist.identity.data.PortalUserData;
import my.gov.perkeso.assist.registration.domain.Employer;
import my.gov.perkeso.assist.registration.domain.EmployerRepository;
import my.gov.perkeso.assist.registration.domain.PortalUser;
import my.gov.perkeso.assist.registration.domain.PortalUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortalEnrollmentWritePlatformService {

    private final PortalUserRepository portalUserRepository;
    private final EmployerRepository employerRepository;

    @Transactional
    public PortalUserData enroll(final String username, final String email, final PortalApplicationType applicationType,
            final String employerCode) {
        requireNonBlank(username, "username");
        requireNonBlank(email, "email");

        portalUserRepository.findByUsernameIgnoreCase(username.trim()).ifPresent(existing -> {
            throw new IllegalArgumentException("Portal username already exists: " + username);
        });
        portalUserRepository.findByEmailIgnoreCase(email.trim()).ifPresent(existing -> {
            throw new IllegalArgumentException("Portal email already exists: " + email);
        });

        final PortalUser portalUser = new PortalUser();
        portalUser.setUsername(username.trim());
        portalUser.setEmail(email.trim());
        portalUser.setApplicationType(applicationType.name());
        portalUser.setEnrolledDate(LocalDateTime.now());

        if (applicationType == PortalApplicationType.EXISTING_EMPLOYER) {
            requireNonBlank(employerCode, "employerCode");
            final Employer employer = employerRepository.findByEmployerCodeAndDeletedFalse(employerCode.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Employer not found: " + employerCode));
            portalUser.setEmployerId(employer.getId());
            portalUser.setEmployerCode(employer.getEmployerCode());
            portalUser.setLinkedDate(LocalDateTime.now());
        }

        return toData(portalUserRepository.save(portalUser));
    }

    private static PortalUserData toData(final PortalUser portalUser) {
        return PortalUserData.builder().id(portalUser.getId()).username(portalUser.getUsername())
                .email(portalUser.getEmail()).applicationType(portalUser.getApplicationType())
                .employerId(portalUser.getEmployerId()).employerCode(portalUser.getEmployerCode())
                .enrolledDate(portalUser.getEnrolledDate()).linkedDate(portalUser.getLinkedDate()).build();
    }

    private static void requireNonBlank(final String value, final String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
