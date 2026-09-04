package my.gov.perkeso.assist.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import my.gov.perkeso.assist.registration.domain.AppStatus;
import my.gov.perkeso.assist.registration.domain.BusinessInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalesTaxRejectionLetterAttributeBuilderTest {

    @Mock
    private RegistrationLetterCommonAttributeBuilder commonAttributeBuilder;

    private SalesTaxRejectionLetterAttributeBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new SalesTaxRejectionLetterAttributeBuilder(commonAttributeBuilder);
        when(commonAttributeBuilder.buildCommonAttributes(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of("refNoOur", "CASE-001"));
    }

    @Test
    void buildAttributes_includesEmployerRejectionRow() {
        final BusinessInfo businessInfo = new BusinessInfo();
        businessInfo.setRegistrationNo("201901234567");

        final TempEmployer tempEmployer = new TempEmployer();
        tempEmployer.setEmployerName("ABC Manufacturing Sdn Bhd");
        tempEmployer.setBusinessInfo(businessInfo);
        tempEmployer.setPksBranchId(2L);

        final RegGeneralInfo regCase = new RegGeneralInfo();
        regCase.setCaseRefNo("CASE-001");
        regCase.setAppStatus(AppStatus.REJECTED);
        regCase.setSectionId(RegistrationSection.REG_NEW_REG_SST_SALES_TAX.getAssistSectionId());
        regCase.setTempEmployer(tempEmployer);
        regCase.setAppStatusReason("Duplicate BRN");

        final Map<String, String> attributes = builder.buildAttributes(regCase);

        assertThat(attributes.get("employerList")).contains("ABC Manufacturing Sdn Bhd");
        assertThat(attributes.get("employerList")).contains("201901234567");
        assertThat(attributes.get("employerList")).contains("Duplicate BRN");
    }
}
