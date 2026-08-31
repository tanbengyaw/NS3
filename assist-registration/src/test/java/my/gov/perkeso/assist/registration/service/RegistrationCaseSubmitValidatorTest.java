package my.gov.perkeso.assist.registration.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import my.gov.perkeso.assist.registration.constant.DataSource;
import my.gov.perkeso.assist.registration.domain.AppStatus;
import my.gov.perkeso.assist.registration.domain.BusinessInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import my.gov.perkeso.assist.registration.exception.RegistrationCaseSubmitValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationCaseSubmitValidatorTest {

    @Mock
    private TempSstInfoWritePlatformService tempSstInfoWritePlatformService;

    private RegistrationCaseSubmitValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RegistrationCaseSubmitValidator(tempSstInfoWritePlatformService);
    }

    @Test
    void acceptsCompletePortalCase() {
        assertThatCode(() -> validator.validateForSubmit(completePortalCase())).doesNotThrowAnyException();
    }

    @Test
    void rejectsPortalCaseWithoutEmail() {
        final RegGeneralInfo regCase = completePortalCase();
        regCase.getTempEmployer().setEmail(null);

        assertThatThrownBy(() -> validator.validateForSubmit(regCase))
                .isInstanceOf(RegistrationCaseSubmitValidationException.class)
                .hasMessageContaining("email");
    }

    @Test
    void rejectsCaseWithoutAddressLine1() {
        final RegGeneralInfo regCase = completePortalCase();
        regCase.getTempEmployer().setAddressLine1(" ");

        assertThatThrownBy(() -> validator.validateForSubmit(regCase))
                .isInstanceOf(RegistrationCaseSubmitValidationException.class)
                .hasMessageContaining("addressLine1");
    }

    private static RegGeneralInfo completePortalCase() {
        final BusinessInfo businessInfo = new BusinessInfo();
        businessInfo.setRegistrationNo("201901234567");
        businessInfo.setBusinessEntityTypeId(1L);

        final TempEmployer tempEmployer = new TempEmployer();
        tempEmployer.setEmployerName("Acme Sdn Bhd");
        tempEmployer.setBusinessInfo(businessInfo);
        tempEmployer.setServiceTypeId(1L);
        tempEmployer.setPksBranchId(2L);
        tempEmployer.setPostCode("50812");
        tempEmployer.setEmail("hr@acme.example");
        tempEmployer.setAddressLine1("No 1 Jalan Example");

        final RegGeneralInfo regCase = new RegGeneralInfo();
        regCase.setAppStatus(AppStatus.NEW);
        regCase.setDataSourceId(DataSource.PORTAL.getAssistId());
        regCase.setTempEmployer(tempEmployer);
        return regCase;
    }
}
