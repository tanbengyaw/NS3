package my.gov.perkeso.assist.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.Map;
import my.gov.perkeso.assist.registration.domain.BusinessInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class RegistrationLetterCommonAttributeBuilderTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private AreaCodeLookupService areaCodeLookupService;

    @Mock
    private BranchReferenceReadPlatformService branchReferenceReadPlatformService;

    @Mock
    private SalesTaxAcknowledgementLetterTemplateRenderer templateRenderer;

    private RegistrationLetterCommonAttributeBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new RegistrationLetterCommonAttributeBuilder(jdbcTemplate, areaCodeLookupService,
                branchReferenceReadPlatformService, templateRenderer);
    }

    @Test
    void buildCommonAttributes_usesBranchStateAndAreaCodeInReference() {
        final RegGeneralInfo regCase = new RegGeneralInfo();
        regCase.setId(1L);
        regCase.setCaseRefNo("CRN09202600000001");

        final TempEmployer tempEmployer = new TempEmployer();
        tempEmployer.setEmployerName("Acme Sdn Bhd");
        tempEmployer.setPksBranchId(2L);
        tempEmployer.setPostCode("50812");
        tempEmployer.setStateId(12L);
        tempEmployer.setAddressLine1("No 1 Jalan Test");
        tempEmployer.setBusinessInfo(new BusinessInfo());
        regCase.setTempEmployer(tempEmployer);

        when(branchReferenceReadPlatformService.retrieveBranchForLetter(2L))
                .thenReturn(BranchReferenceReadPlatformService.BranchLetterData.builder()
                        .id(2L)
                        .name("Pejabat PERKESO Shah Alam")
                        .addressLine1("Tingkat 1, Wisma PERKESO")
                        .addressLine2("Persiaran Raja")
                        .postCode("40000")
                        .cityName("Shah Alam")
                        .stateName("Selangor")
                        .fax("03-5544 6100")
                        .build());
        when(areaCodeLookupService.findAreaCodeByPostCodeAndBranchId("50812", 2L)).thenReturn("A37");
        when(templateRenderer.renderHeader(org.mockito.ArgumentMatchers.anyMap()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    final Map<String, String> header = invocation.getArgument(0);
                    assertThat(header.get("perkesoState")).isEqualTo("Selangor");
                    assertThat(header.get("NoFaks")).isEqualTo("03-5544 6100");
                    return "<header/>";
                });

        final Map<String, String> attributes = builder.buildCommonAttributes(regCase);

        assertThat(attributes.get("refNoOur")).isEqualTo("208/A37/CRN09202600000001");
        assertThat(attributes.get("branchName")).isEqualTo("Pejabat PERKESO Shah Alam");
        assertThat(attributes.get("branchTitleWithState")).isEqualTo("b.p : Pengarah Negeri Selangor");
        assertThat(attributes.get("declareHeader")).isEqualTo("<header/>");
    }
}
