package my.gov.perkeso.assist.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import my.gov.perkeso.assist.core.commands.api.JsonCommand;
import my.gov.perkeso.assist.core.security.PlatformUser;
import my.gov.perkeso.assist.core.security.PlatformUserContext;
import my.gov.perkeso.assist.core.security.PlatformUserRole;
import my.gov.perkeso.assist.registration.domain.AppStatus;
import my.gov.perkeso.assist.registration.domain.EmployerRepository;
import my.gov.perkeso.assist.registration.domain.EmployerStatusInfoRepository;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfoRepository;
import my.gov.perkeso.assist.registration.exception.RegistrationCaseInvalidStatusException;
import my.gov.perkeso.assist.registration.reference.ReferenceNoGeneratorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationCaseQueryServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RegGeneralInfoRepository regGeneralInfoRepository;
    @Mock
    private EmployerRepository employerRepository;
    @Mock
    private EmployerStatusInfoRepository employerStatusInfoRepository;
    @Mock
    private EmployerCodeGenerator employerCodeGenerator;
    @Mock
    private ReferenceNoGeneratorFactory referenceNoGeneratorFactory;
    @Mock
    private RegistrationCaseContextService registrationCaseContextService;
    @Mock
    private RegistrationCaseSubmitValidator registrationCaseSubmitValidator;
    @Mock
    private RegistrationCaseSubmitRouter registrationCaseSubmitRouter;
    @Mock
    private PortalUserLinkService portalUserLinkService;
    @Mock
    private EmployeePromotionService employeePromotionService;
    @Mock
    private RegistrationSpecialCaseService registrationSpecialCaseService;
    @Mock
    private SstInfoPromotionService sstInfoPromotionService;
    @Mock
    private PlatformUserContext platformUserContext;

    @InjectMocks
    private RegistrationCaseWritePlatformService writeService;

    @Test
    void officerCanSendSubmittedCaseToQuery() throws Exception {
        final RegGeneralInfo regCase = submittedCase();
        when(regGeneralInfoRepository.findById(1L)).thenReturn(Optional.of(regCase));
        when(platformUserContext.getCurrentUser()).thenReturn(officerUser());

        final var result = writeService.queryCase(jsonCommand(Map.of("remark", "Please upload SSM cert")));

        assertThat(regCase.getAppStatus()).isEqualTo(AppStatus.IN_QUERY);
        assertThat(regCase.getQueryRemark()).isEqualTo("Please upload SSM cert");
        assertThat(regCase.getInqueryByUsername()).isEqualTo("admin");
        assertThat(regCase.getInqueryDate()).isNotNull();
        assertThat(result.getChanges()).containsEntry("appStatus", "IN_QUERY");
        verify(regGeneralInfoRepository).save(regCase);
    }

    @Test
    void employerCannotSendCaseToQuery() throws Exception {
        when(platformUserContext.getCurrentUser()).thenReturn(new PlatformUser("employer", "hr@acme.example",
                EnumSet.of(PlatformUserRole.EMPLOYER), null));

        assertThatThrownBy(() -> writeService.queryCase(jsonCommand(Map.of("remark", "test"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only officers");
    }

    @Test
    void cannotQueryCaseThatIsNotSubmitted() throws Exception {
        final RegGeneralInfo regCase = submittedCase();
        regCase.setAppStatus(AppStatus.NEW);
        when(regGeneralInfoRepository.findById(1L)).thenReturn(Optional.of(regCase));
        when(platformUserContext.getCurrentUser()).thenReturn(officerUser());

        assertThatThrownBy(() -> writeService.queryCase(jsonCommand(Map.of("remark", "test"))))
                .isInstanceOf(RegistrationCaseInvalidStatusException.class);
    }

    private JsonCommand jsonCommand(final Map<String, Object> fields) throws Exception {
        final var parsed = objectMapper.valueToTree(fields);
        return new JsonCommand(parsed.toString(), parsed, 1L, fields);
    }

    private static RegGeneralInfo submittedCase() {
        final RegGeneralInfo regCase = new RegGeneralInfo();
        regCase.setId(1L);
        regCase.setCaseRefNo("CRN08202600000010");
        regCase.setAppStatus(AppStatus.SUBMITTED);
        return regCase;
    }

    private static PlatformUser officerUser() {
        return new PlatformUser("admin", "officer@perkeso.example", EnumSet.of(PlatformUserRole.OFFICER), 2L);
    }
}
