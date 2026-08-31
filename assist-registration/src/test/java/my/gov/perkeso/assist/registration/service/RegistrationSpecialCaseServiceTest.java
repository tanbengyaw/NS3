package my.gov.perkeso.assist.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import my.gov.perkeso.assist.registration.constant.RegistrationSpecialCaseType;
import my.gov.perkeso.assist.registration.data.RegistrationSpecialCase;
import my.gov.perkeso.assist.registration.domain.BusinessInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationSpecialCaseServiceTest {

    @Mock
    private EmployerCodeGenerator employerCodeGenerator;

    @InjectMocks
    private RegistrationSpecialCaseService specialCaseService;

    @Test
    void detectsDuplicateRegistrationNumberForNonBranchEmployer() {
        when(employerCodeGenerator.isRegistrationNoRegistered("201901234567")).thenReturn(true);

        final List<RegistrationSpecialCase> specialCases = specialCaseService.checkOnSubmit(caseWithBrn("201901234567", false));

        assertThat(specialCases).hasSize(1);
        assertThat(specialCases.get(0).getType()).isEqualTo(RegistrationSpecialCaseType.DUPLICATE_REGISTRATION_NUMBER);
    }

    @Test
    void skipsDuplicateCheckForBranchEmployer() {
        final List<RegistrationSpecialCase> specialCases = specialCaseService.checkOnSubmit(caseWithBrn("201901234567", true));

        assertThat(specialCases).isEmpty();
    }

    private static RegGeneralInfo caseWithBrn(final String brn, final boolean branch) {
        final BusinessInfo businessInfo = new BusinessInfo();
        businessInfo.setRegistrationNo(brn);

        final TempEmployer tempEmployer = new TempEmployer();
        tempEmployer.setBranch(branch);
        tempEmployer.setBusinessInfo(businessInfo);

        final RegGeneralInfo regCase = new RegGeneralInfo();
        regCase.setTempEmployer(tempEmployer);
        return regCase;
    }
}
