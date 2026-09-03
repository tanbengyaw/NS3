package my.gov.perkeso.assist.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import my.gov.perkeso.assist.registration.domain.AppStatus;
import my.gov.perkeso.assist.registration.domain.BusinessInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfoRepository;
import my.gov.perkeso.assist.registration.domain.SstInfo;
import my.gov.perkeso.assist.registration.domain.SstInfoRepository;
import my.gov.perkeso.assist.registration.domain.TempEmployer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalesTaxAcknowledgementLetterServiceTest {

    @Mock
    private RegGeneralInfoRepository regGeneralInfoRepository;

    @Mock
    private SstInfoRepository sstInfoRepository;

    @Mock
    private SalesTaxAcknowledgementLetterAttributeBuilder attributeBuilder;

    @Mock
    private SalesTaxAcknowledgementLetterTemplateRenderer templateRenderer;

    @Mock
    private SalesTaxAcknowledgementLetterPdfRenderer pdfRenderer;

    private SalesTaxAcknowledgementLetterService service;

    @BeforeEach
    void setUp() {
        service = new SalesTaxAcknowledgementLetterService(regGeneralInfoRepository, sstInfoRepository,
                attributeBuilder, templateRenderer, pdfRenderer);
    }

    @Test
    void generateAcknowledgementLetter_pdfUsesLegacyTemplateContent() {
        stubApprovedCase();
        when(attributeBuilder.buildAttributes(any(), any())).thenReturn(sampleAttributes());
        when(templateRenderer.render(sampleAttributes())).thenReturn("<html>KELULUSAN PENDAFTARAN</html>");
        when(templateRenderer.templateBaseUri()).thenReturn("file:/templates/legacy/");
        when(pdfRenderer.renderPdf("<html>KELULUSAN PENDAFTARAN</html>", "file:/templates/legacy/"))
                .thenReturn("%PDF-test".getBytes());

        final SalesTaxAcknowledgementLetterService.SalesTaxAcknowledgementLetter letter =
                service.generateAcknowledgementLetter(42L, SalesTaxAcknowledgementLetterFormat.PDF);

        assertThat(letter.fileName()).isEqualTo("sales-tax-acknowledgement-CASE-001.pdf");
        assertThat(letter.contentType()).isEqualTo("application/pdf");
        assertThat(new String(letter.content())).startsWith("%PDF");
    }

    @Test
    void generateAcknowledgementLetter_htmlUsesLegacyTemplateRenderer() {
        stubApprovedCase();
        when(attributeBuilder.buildAttributes(any(), any())).thenReturn(sampleAttributes());
        when(templateRenderer.render(sampleAttributes()))
                .thenReturn("<html>KELULUSAN PENDAFTARAN DI BAWAH SEKSYEN 13</html>");

        final SalesTaxAcknowledgementLetterService.SalesTaxAcknowledgementLetter letter =
                service.generateAcknowledgementLetter(42L, SalesTaxAcknowledgementLetterFormat.HTML);

        assertThat(letter.fileName()).isEqualTo("sales-tax-acknowledgement-CASE-001.html");
        assertThat(new String(letter.content())).contains("KELULUSAN PENDAFTARAN");
    }

    @Test
    void generateAcknowledgementLetter_rejectsNonApprovedCase() {
        final RegGeneralInfo regCase = approvedSalesTaxCase();
        regCase.setAppStatus(AppStatus.SUBMITTED);
        when(regGeneralInfoRepository.findById(42L)).thenReturn(java.util.Optional.of(regCase));

        assertThatThrownBy(() -> service.generateAcknowledgementLetter(42L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("approved");
    }

    private void stubApprovedCase() {
        when(regGeneralInfoRepository.findById(42L)).thenReturn(java.util.Optional.of(approvedSalesTaxCase()));
        when(sstInfoRepository.findByRegGeneralInfoIdAndDeletedFalse(42L)).thenReturn(List.of(promotedSstInfo()));
    }

    private static Map<String, String> sampleAttributes() {
        return Map.of(
                "refNoOur", "CASE-001",
                "refNoYour", "KL-CJ-00001234/2024",
                "branchOffice", "Jabatan Kastam Diraja Malaysia Kuala Lumpur");
    }

    private static RegGeneralInfo approvedSalesTaxCase() {
        final BusinessInfo businessInfo = new BusinessInfo();
        businessInfo.setRegistrationNo("201901234567");

        final TempEmployer tempEmployer = new TempEmployer();
        tempEmployer.setEmployerName("Sales Tax Manufacturing Sdn Bhd");
        tempEmployer.setBusinessInfo(businessInfo);
        tempEmployer.setAddressLine1("Lot 1 Industrial Park");
        tempEmployer.setCityName("Kuala Lumpur");
        tempEmployer.setPostCode("50812");
        tempEmployer.setPksBranchId(2L);

        final RegGeneralInfo regCase = new RegGeneralInfo();
        regCase.setId(42L);
        regCase.setCaseRefNo("CASE-001");
        regCase.setAppStatus(AppStatus.APPROVED);
        regCase.setSectionId(RegistrationSection.REG_NEW_REG_SST_SALES_TAX.getAssistSectionId());
        regCase.setEmployerId(99L);
        regCase.setTempEmployer(tempEmployer);
        regCase.setUpdatedDate(LocalDateTime.of(2024, 6, 15, 10, 0));
        regCase.setDocumentReceivedDate(LocalDateTime.of(2024, 6, 1, 10, 0));
        return regCase;
    }

    private static SstInfo promotedSstInfo() {
        final SstInfo sstInfo = new SstInfo();
        sstInfo.setTradeName("Sales Tax Manufacturing");
        sstInfo.setSalesTaxSmkRegNo("KL-CJ-00001234/2024");
        sstInfo.setApplicantName("Tan Ah Kow");
        sstInfo.setBusinessComDate(LocalDate.of(2024, 1, 1));
        sstInfo.setFinYrEndMon(9);
        sstInfo.setCreatedDate(LocalDateTime.of(2024, 6, 15, 10, 0));
        return sstInfo;
    }
}
