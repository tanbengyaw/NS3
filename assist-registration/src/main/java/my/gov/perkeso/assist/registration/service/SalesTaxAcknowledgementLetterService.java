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
    private final SalesTaxAcknowledgementLetterAttributeBuilder acknowledgementAttributeBuilder;
    private final SalesTaxInquiryLetterAttributeBuilder inquiryAttributeBuilder;
    private final SalesTaxRejectionLetterAttributeBuilder rejectionAttributeBuilder;
    private final SalesTaxAcknowledgementLetterTemplateRenderer templateRenderer;
    private final SalesTaxAcknowledgementLetterPdfRenderer pdfRenderer;

    @Transactional(readOnly = true)
    public SalesTaxAcknowledgementLetter generateAcknowledgementLetter(final Long caseId) {
        return generateLetter(caseId, SalesTaxLetterType.ACKNOWLEDGEMENT, SalesTaxAcknowledgementLetterFormat.PDF);
    }

    @Transactional(readOnly = true)
    public SalesTaxAcknowledgementLetter generateAcknowledgementLetter(final Long caseId,
            final SalesTaxAcknowledgementLetterFormat format) {
        return generateLetter(caseId, SalesTaxLetterType.ACKNOWLEDGEMENT, format);
    }

    @Transactional(readOnly = true)
    public SalesTaxAcknowledgementLetter generateLetter(final Long caseId, final SalesTaxLetterType letterType,
            final SalesTaxAcknowledgementLetterFormat format) {
        final RegGeneralInfo regCase = loadSalesTaxCase(caseId);
        assertLetterStatus(regCase, letterType);

        final Map<String, String> attributes = buildAttributes(regCase, letterType);
        final String html = renderHtml(attributes, letterType);
        final String safeRef = regCase.getCaseRefNo().replace('/', '-');
        final String fileName = letterType.filePrefix() + "-" + safeRef + format.fileExtension();
        final byte[] content = format == SalesTaxAcknowledgementLetterFormat.PDF
                ? pdfRenderer.renderPdf(html, templateRenderer.templateBaseUri())
                : html.getBytes(StandardCharsets.UTF_8);
        return new SalesTaxAcknowledgementLetter(fileName, format.contentType(), content);
    }

    private RegGeneralInfo loadSalesTaxCase(final Long caseId) {
        final RegGeneralInfo regCase = regGeneralInfoRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration case not found: " + caseId));
        assertSalesTaxSection(regCase);
        return regCase;
    }

    private Map<String, String> buildAttributes(final RegGeneralInfo regCase, final SalesTaxLetterType letterType) {
        return switch (letterType) {
            case ACKNOWLEDGEMENT -> {
                final SstInfo sstInfo = sstInfoRepository.findByRegGeneralInfoIdAndDeletedFalse(regCase.getId()).stream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "Approved sales tax case has no promoted SST info: " + regCase.getId()));
                yield acknowledgementAttributeBuilder.buildAttributes(regCase, sstInfo);
            }
            case INQUIRY -> inquiryAttributeBuilder.buildAttributes(regCase);
            case REJECTION -> rejectionAttributeBuilder.buildAttributes(regCase);
        };
    }

    private String renderHtml(final Map<String, String> attributes, final SalesTaxLetterType letterType) {
        return switch (letterType) {
            case ACKNOWLEDGEMENT -> templateRenderer.renderAcknowledgement(attributes);
            case INQUIRY -> templateRenderer.renderInquiry(attributes);
            case REJECTION -> templateRenderer.renderRejection(attributes);
        };
    }

    private static void assertLetterStatus(final RegGeneralInfo regCase, final SalesTaxLetterType letterType) {
        if (regCase.getAppStatus() != letterType.requiredStatus()) {
            throw new IllegalArgumentException(letterType.queryValue() + " letter is only available for "
                    + letterType.requiredStatus().name() + " cases (current status: "
                    + regCase.getAppStatus().name() + ")");
        }
    }

    private static void assertSalesTaxSection(final RegGeneralInfo regCase) {
        if (regCase.getSectionId() != RegistrationSection.REG_NEW_REG_SST_SALES_TAX.getAssistSectionId()) {
            throw new IllegalArgumentException(
                    "Sales tax letters are only available for sales tax new registration (1100)");
        }
    }

    public record SalesTaxAcknowledgementLetter(String fileName, String contentType, byte[] content) {
    }
}
