package my.gov.perkeso.assist.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import my.gov.perkeso.assist.core.security.PlatformUser;
import my.gov.perkeso.assist.core.security.PlatformUserContext;
import my.gov.perkeso.assist.core.security.PlatformUserRole;
import my.gov.perkeso.assist.identity.constant.PortalApplicationType;
import my.gov.perkeso.assist.identity.data.PortalEnrollmentRequest;
import my.gov.perkeso.assist.registration.domain.Employer;
import my.gov.perkeso.assist.registration.domain.EmployerRepository;
import my.gov.perkeso.assist.registration.domain.PortalUser;
import my.gov.perkeso.assist.registration.domain.PortalUserRepository;
import my.gov.perkeso.assist.registration.domain.base.UserEmployer;
import my.gov.perkeso.assist.registration.service.BaseUserEmployerEnrollmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortalEnrollmentWritePlatformServiceTest {

    @Mock
    private PortalUserRepository portalUserRepository;

    @Mock
    private EmployerRepository employerRepository;

    @Mock
    private BaseUserEmployerEnrollmentService baseUserEmployerEnrollmentService;

    @Mock
    private PlatformUserContext platformUserContext;

    @InjectMocks
    private PortalEnrollmentWritePlatformService writeService;

    @Test
    void enrollsNewEmployerPortalUser() {
        when(portalUserRepository.findByUsernameIgnoreCase("portal1")).thenReturn(Optional.empty());
        when(portalUserRepository.findByEmailIgnoreCase("portal1@example.com")).thenReturn(Optional.empty());
        when(portalUserRepository.save(any(PortalUser.class))).thenAnswer(invocation -> {
            final PortalUser user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(10L);
            }
            return user;
        });
        when(baseUserEmployerEnrollmentService.submitEnrollment(any())).thenAnswer(invocation -> {
            final UserEmployer userEmployer = new UserEmployer();
            userEmployer.setId(100L);
            return userEmployer;
        });

        final var result = writeService.enroll(sampleRequest("portal1", "portal1@example.com",
                PortalApplicationType.NEW_EMPLOYER.name(), null));

        assertThat(result.getApplicationType()).isEqualTo("NEW_EMPLOYER");
        assertThat(result.getEmployerId()).isNull();
        assertThat(result.getEnrolledDate()).isNotNull();
        assertThat(result.getFullName()).isEqualTo("Ali Bin Abu");
        assertThat(result.getEnrollmentStatus()).isEqualTo("SUBMITTED");

        final ArgumentCaptor<PortalUser> captor = ArgumentCaptor.forClass(PortalUser.class);
        verify(portalUserRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(1).getUserEmployerId()).isEqualTo(100L);
    }

    @Test
    void enrollsExistingEmployerPortalUser() {
        final Employer employer = new Employer();
        employer.setId(99L);
        employer.setEmployerCode("A3700000001F");

        when(portalUserRepository.findByUsernameIgnoreCase("hr1")).thenReturn(Optional.empty());
        when(portalUserRepository.findByEmailIgnoreCase("hr1@example.com")).thenReturn(Optional.empty());
        when(employerRepository.findByEmployerCodeAndDeletedFalse("A3700000001F")).thenReturn(Optional.of(employer));
        when(portalUserRepository.save(any(PortalUser.class))).thenAnswer(invocation -> {
            final PortalUser user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(11L);
            }
            return user;
        });
        when(baseUserEmployerEnrollmentService.submitEnrollment(any())).thenAnswer(invocation -> {
            final UserEmployer userEmployer = new UserEmployer();
            userEmployer.setId(101L);
            userEmployer.setEmployerId(99L);
            return userEmployer;
        });

        final var result = writeService.enroll(sampleRequest("hr1", "hr1@example.com",
                PortalApplicationType.EXISTING_EMPLOYER.name(), "A3700000001F"));

        assertThat(result.getApplicationType()).isEqualTo("EXISTING_EMPLOYER");
        assertThat(result.getEmployerId()).isEqualTo(99L);
        assertThat(result.getEmployerCode()).isEqualTo("A3700000001F");
        assertThat(result.getLinkedDate()).isNotNull();

        final ArgumentCaptor<PortalUser> captor = ArgumentCaptor.forClass(PortalUser.class);
        verify(portalUserRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(1).getEmployerCode()).isEqualTo("A3700000001F");
        assertThat(captor.getAllValues().get(1).getUserEmployerId()).isEqualTo(101L);
    }

    private static PortalEnrollmentRequest sampleRequest(final String username, final String email,
            final String applicationType, final String employerCode) {
        return PortalEnrollmentRequest.builder()
                .username(username)
                .email(email)
                .applicationType(applicationType)
                .employerCode(employerCode)
                .employerName("Acme Sdn Bhd")
                .registrationTypeId(1L)
                .registrationNo("201901012345")
                .addressLine1("No 1 Jalan Test")
                .stateId(14L)
                .cityId(1401L)
                .cityName("Kuala Lumpur")
                .postCode("50450")
                .fullName("Ali Bin Abu")
                .identificationTypeId(2L)
                .identificationNo("900101011234")
                .phoneCallingCode("+60")
                .phoneNumber("123456789")
                .securityPhrase("My secret phrase")
                .build();
    }

    private static PlatformUser officerUser() {
        return new PlatformUser("admin", "admin@example.com", Set.of(PlatformUserRole.ADMIN), 1L);
    }
}