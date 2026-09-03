package my.gov.perkeso.assist.registration.service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException;
import my.gov.perkeso.assist.registration.constant.RegistrationSection;
import my.gov.perkeso.assist.registration.domain.AppStatus;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfoRepository;
import my.gov.perkeso.assist.registration.domain.SstInfo;
import my.gov.perkeso.assist.registration.domain.SstInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SalesTaxAcknowledgementLetterService {

    private final RegGeneralInfoRepository regGeneralInfoRepository;
    private final SstInfoRepository sstInfoRepository;
    private final SalesTaxAcknowledgementLetterAttributeBuilder attributeBuilder;
    private final SalesTaxAcknowledgementLetterTemplateRenderer templateRenderer;
    private final SalesTaxAcknowledgementLetterPdfRenderer pdfRenderer;

    @Transactional(readOnly = true)
    public SalesTaxAcknowledgementLetter generateAcknowledgementLetter(final Long caseId) {
        return generateAcknowledgementLetter(caseId, SalesTaxAcknowledgementLetterFormat.PDF);
    }

    @Transactional(readOnly = true)
    public SalesTaxAcknowledgementLetter generateAcknowledgementLetter(final Long caseId,
            final SalesTaxAcknowledgementLetterFormat format) {
        final RegGeneralInfo regCase = regGeneralInfoRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration case not found: " + caseId));
        assertSalesTaxSection(regCase);
        if (regCase.getAppStatus() != AppStatus.APPROVED) {
            throw new IllegalArgumentException(
                    "Acknowledgement letter is only available for approved cases (current status: "
                            + regCase.getAppStatus().name() + ")");
        }

        final SstInfo sstInfo = sstInfoRepository.findByRegGeneralInfoIdAndDeletedFalse(caseId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Approved sales tax case has no promoted SST info: " + caseId));

        final Map<String, String> attributes = attributeBuilder.buildAttributes(regCase, sstInfo);
        final String html = templateRenderer.render(attributes);
        final String safeRef = regCase.getCaseRefNo().replace('/', '-');
        final String fileName = "sales-tax-acknowledgement-" + safeRef + format.fileExtension();
        final byte[] content = format == SalesTaxAcknowledgementLetterFormat.PDF
                ? pdfRenderer.renderPdf(html, templateRenderer.templateBaseUri())
                : html.getBytes(StandardCharsets.UTF_8);
        return new SalesTaxAcknowledgementLetter(fileName, format.contentType(), content);
    }

    private static void assertSalesTaxSection(final RegGeneralInfo regCase) {
        if (regCase.getSectionId() != RegistrationSection.REG_NEW_REG_SST_SALES_TAX.getAssistSectionId()) {
            throw new IllegalArgumentException(
                    "Acknowledgement letter is only available for sales tax new registration (1100)");
        }
    }

    public record SalesTaxAcknowledgementLetter(String fileName, String contentType, byte[] content) {
    }
}
