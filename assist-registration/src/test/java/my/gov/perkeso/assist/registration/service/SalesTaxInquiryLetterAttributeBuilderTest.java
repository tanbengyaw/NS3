package my.gov.perkeso.assist.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import my.gov.perkeso.assist.registration.data.SupportingDocumentTypeData;
import my.gov.perkeso.assist.registration.domain.AppStatus;
import my.gov.perkeso.assist.registration.domain.BusinessInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import my.gov.perkeso.assist.registration.domain.TempSstInfoRepository;
import my.gov.perkeso.assist.registration.domain.TempSstSupportingDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalesTaxInquiryLetterAttributeBuilderTest {

    @Mock
    private RegistrationLetterCommonAttributeBuilder commonAttributeBuilder;

    @Mock
    private RegistrationReferenceReadPlatformService registrationReferenceReadPlatformService;

    @Mock
    private TempSstInfoRepository tempSstInfoRepository;

    @Mock
    private TempSstSupportingDocumentRepository tempSstSupportingDocumentRepository;

    private SalesTaxInquiryLetterAttributeBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new SalesTaxInquiryLetterAttributeBuilder(commonAttributeBuilder,
                registrationReferenceReadPlatformService, tempSstInfoRepository,
                tempSstSupportingDocumentRepository);
        when(commonAttributeBuilder.buildCommonAttributes(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of("refNoOur", "CASE-001"));
    }

    @Test
    void buildAttributes_includesOrderListWithQueryRemark() {
        final RegGeneralInfo regCase = salesTaxCase();
        regCase.setQueryRemark("Please upload SSM certificate");
        when(registrationReferenceReadPlatformService.retrieveSupportingDocumentTypesForSalesTax())
                .thenReturn(List.of(SupportingDocumentTypeData.builder().id(1L).label("SSM certificate").build()));
        when(tempSstInfoRepository.findByTempEmployerId(regCase.getTempEmployer().getId()))
                .thenReturn(java.util.Optional.empty());

        final Map<String, String> attributes = builder.buildAttributes(regCase);

        assertThat(attributes.get("orderList")).contains("SSM certificate");
        assertThat(attributes.get("orderList")).contains("CATATAN: Please upload SSM certificate");
        assertThat(attributes.get("employeeList")).contains("Tiada maklumat pekerja");
    }

    private static RegGeneralInfo salesTaxCase() {
        final TempEmployer tempEmployer = new TempEmployer();
        tempEmployer.setId(7L);
        tempEmployer.setEmployerName("ABC Manufacturing Sdn Bhd");
        tempEmployer.setBusinessInfo(new BusinessInfo());
        tempEmployer.setPksBranchId(2L);

        final RegGeneralInfo regCase = new RegGeneralInfo();
        regCase.setId(42L);
        regCase.setCaseRefNo("CASE-001");
        regCase.setAppStatus(AppStatus.IN_QUERY);
        regCase.setSectionId(RegistrationSection.REG_NEW_REG_SST_SALES_TAX.getAssistSectionId());
        regCase.setTempEmployer(tempEmployer);
        return regCase;
    }
}
