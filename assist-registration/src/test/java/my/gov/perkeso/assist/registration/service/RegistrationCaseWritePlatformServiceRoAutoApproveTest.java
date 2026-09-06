package my.gov.perkeso.assist.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumSet;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import my.gov.perkeso.assist.core.commands.api.JsonCommand;
import my.gov.perkeso.assist.core.security.PlatformUser;
import my.gov.perkeso.assist.core.security.PlatformUserContext;
import my.gov.perkeso.assist.core.security.PlatformUserRole;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import my.gov.perkeso.assist.registration.domain.AppStatus;
import my.gov.perkeso.assist.registration.domain.BusinessInfo;
import my.gov.perkeso.assist.registration.domain.Employer;
import my.gov.perkeso.assist.registration.domain.EmployerRepository;
import my.gov.perkeso.assist.registration.domain.EmployerStatusInfo;
import my.gov.perkeso.assist.registration.domain.EmployerStatusInfoRepository;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfoRepository;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import my.gov.perkeso.assist.registration.exception.RegistrationBrnDuplicateException;
import my.gov.perkeso.assist.registration.reference.ReferenceNoGeneratorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationCaseWritePlatformServiceRoAutoApproveTest {

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
    private PostcodeBranchRoutingService postcodeBranchRoutingService;
    @Mock
    private PlatformUserContext platformUserContext;

    @InjectMocks
    private RegistrationCaseWritePlatformService writeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void submitByRoAutoApprovesCase() throws Exception {
        final RegGeneralInfo regCase = draftCase();
        final Employer employer = new Employer();
        employer.setId(99L);
        employer.setEmployerCode("A3700000001F");

        when(regGeneralInfoRepository.findById(1L)).thenReturn(Optional.of(regCase));
        when(platformUserContext.getCurrentUser()).thenReturn(roUser());
        when(registrationSpecialCaseService.checkOnSubmit(regCase)).thenReturn(List.of());
        when(registrationCaseSubmitRouter.resolveSubmitStatus(any(), any(), any(), any())).thenReturn(AppStatus.APPROVED);
        when(registrationCaseSubmitRouter.isIncompleteSubmit(any())).thenReturn(false);
        when(employerCodeGenerator.promoteFromTempEmployer(regCase.getTempEmployer(), regCase)).thenReturn(employer);
        when(employerRepository.save(employer)).thenReturn(employer);
        when(employerCodeGenerator.createActiveStatus(99L)).thenReturn(new EmployerStatusInfo());

        final var result = writeService.submitCase(jsonCommand(Map.of()));

        assertThat(regCase.getAppStatus()).isEqualTo(AppStatus.APPROVED);
        assertThat(regCase.getEmployerId()).isEqualTo(99L);
        assertThat(result.getResourceId()).isEqualTo(99L);
        assertThat(result.getResourceIdentifier()).isEqualTo("A3700000001F");
        verify(portalUserLinkService).linkEmployerOnApprove(regCase, employer);
        verify(employeePromotionService).promoteOnApprove(5L, employer);
    }

    @Test
    void submitByRoRejectsDuplicateBrn() throws Exception {
        final RegGeneralInfo regCase = draftCase();

        when(regGeneralInfoRepository.findById(1L)).thenReturn(Optional.of(regCase));
        when(platformUserContext.getCurrentUser()).thenReturn(roUser());
        when(registrationSpecialCaseService.checkOnSubmit(regCase)).thenReturn(List.of());
        when(registrationCaseSubmitRouter.resolveSubmitStatus(any(), any(), any(), any())).thenReturn(AppStatus.APPROVED);
        when(registrationCaseSubmitRouter.isIncompleteSubmit(any())).thenReturn(false);
        when(employerCodeGenerator.promoteFromTempEmployer(regCase.getTempEmployer(), regCase))
                .thenThrow(new RegistrationBrnDuplicateException("201901234567"));

        final var result = writeService.submitCase(jsonCommand(Map.of()));

        assertThat(regCase.getAppStatus()).isEqualTo(AppStatus.REJECTED);
        assertThat(regCase.getAppStatusReason()).contains("201901234567");
        assertThat(result.getChanges()).containsEntry("appStatus", "REJECTED");
        verify(regGeneralInfoRepository).save(regCase);
    }

    private static RegGeneralInfo draftCase() {
        final BusinessInfo businessInfo = new BusinessInfo();
        businessInfo.setRegistrationNo("201901234567");

        final TempEmployer tempEmployer = new TempEmployer();
        tempEmployer.setId(5L);
        tempEmployer.setBusinessInfo(businessInfo);

        final RegGeneralInfo regCase = new RegGeneralInfo();
        regCase.setId(1L);
        regCase.setCaseRefNo("CRN08202600000001");
        regCase.setAppStatus(AppStatus.NEW);
        regCase.setSectionId(RegistrationSection.REG_NEW_REG.getAssistSectionId());
        regCase.setTempEmployer(tempEmployer);
        return regCase;
    }

    private static PlatformUser roUser() {
        return new PlatformUser("ro", "ro@perkeso.example", EnumSet.of(PlatformUserRole.RO), 2L);
    }

    private JsonCommand jsonCommand(final Map<String, Object> fields) throws Exception {
        final var parsed = objectMapper.valueToTree(fields);
        return new JsonCommand(parsed.toString(), parsed, 1L, fields);
    }
}
