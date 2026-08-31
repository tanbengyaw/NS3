package my.gov.perkeso.assist.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import my.gov.perkeso.assist.identity.constant.PortalApplicationType;
import my.gov.perkeso.assist.registration.domain.Employer;
import my.gov.perkeso.assist.registration.domain.EmployerRepository;
import my.gov.perkeso.assist.registration.domain.PortalUser;
import my.gov.perkeso.assist.registration.domain.PortalUserRepository;
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

    @InjectMocks
    private PortalEnrollmentWritePlatformService writeService;

    @Test
    void enrollsNewEmployerPortalUser() {
        when(portalUserRepository.findByUsernameIgnoreCase("portal1")).thenReturn(Optional.empty());
        when(portalUserRepository.findByEmailIgnoreCase("portal1@example.com")).thenReturn(Optional.empty());
        when(portalUserRepository.save(any(PortalUser.class))).thenAnswer(invocation -> {
            final PortalUser user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });

        final var result = writeService.enroll("portal1", "portal1@example.com", PortalApplicationType.NEW_EMPLOYER,
                null);

        assertThat(result.getApplicationType()).isEqualTo("NEW_EMPLOYER");
        assertThat(result.getEmployerId()).isNull();
        assertThat(result.getEnrolledDate()).isNotNull();
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
            user.setId(11L);
            return user;
        });

        final var result = writeService.enroll("hr1", "hr1@example.com", PortalApplicationType.EXISTING_EMPLOYER,
                "A3700000001F");

        assertThat(result.getApplicationType()).isEqualTo("EXISTING_EMPLOYER");
        assertThat(result.getEmployerId()).isEqualTo(99L);
        assertThat(result.getEmployerCode()).isEqualTo("A3700000001F");
        assertThat(result.getLinkedDate()).isNotNull();

        final ArgumentCaptor<PortalUser> captor = ArgumentCaptor.forClass(PortalUser.class);
        verify(portalUserRepository).save(captor.capture());
        assertThat(captor.getValue().getEmployerCode()).isEqualTo("A3700000001F");
    }
}
