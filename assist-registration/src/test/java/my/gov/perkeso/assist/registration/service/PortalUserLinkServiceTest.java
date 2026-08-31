package my.gov.perkeso.assist.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import my.gov.perkeso.assist.registration.constant.DataSource;
import my.gov.perkeso.assist.registration.domain.Employer;
import my.gov.perkeso.assist.registration.domain.PortalUser;
import my.gov.perkeso.assist.registration.domain.PortalUserRepository;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortalUserLinkServiceTest {

    @Mock
    private PortalUserRepository portalUserRepository;

    @InjectMocks
    private PortalUserLinkService portalUserLinkService;

    @Test
    void linksPortalUserByEmailOnApprove() {
        final PortalUser portalUser = new PortalUser();
        portalUser.setUsername("employer");
        portalUser.setEmail("hr@acme.example");

        when(portalUserRepository.findByEmailIgnoreCase("hr@acme.example")).thenReturn(Optional.of(portalUser));

        final RegGeneralInfo regCase = portalCase();
        final Employer employer = new Employer();
        employer.setId(99L);
        employer.setEmployerCode("A3700000001F");

        portalUserLinkService.linkEmployerOnApprove(regCase, employer);

        final ArgumentCaptor<PortalUser> captor = ArgumentCaptor.forClass(PortalUser.class);
        verify(portalUserRepository).save(captor.capture());
        assertThat(captor.getValue().getEmployerId()).isEqualTo(99L);
        assertThat(captor.getValue().getEmployerCode()).isEqualTo("A3700000001F");
        assertThat(captor.getValue().getLinkedDate()).isNotNull();
    }

    @Test
    void skipsNonPortalCases() {
        final RegGeneralInfo regCase = portalCase();
        regCase.setDataSourceId(DataSource.OTC.getAssistId());

        portalUserLinkService.linkEmployerOnApprove(regCase, new Employer());

        verify(portalUserRepository, org.mockito.Mockito.never()).save(any());
    }

    private static RegGeneralInfo portalCase() {
        final TempEmployer tempEmployer = new TempEmployer();
        tempEmployer.setEmail("hr@acme.example");

        final RegGeneralInfo regCase = new RegGeneralInfo();
        regCase.setCaseRefNo("CRN08202600000001");
        regCase.setDataSourceId(DataSource.PORTAL.getAssistId());
        regCase.setTempEmployer(tempEmployer);
        return regCase;
    }
}
