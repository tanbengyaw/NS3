package my.gov.perkeso.assist.registration.service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException;
import my.gov.perkeso.assist.registration.constant.RegistrationSectionRouting;
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
        final RegGeneralInfo regCase = loadSstNewRegCase(caseId);
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

    private RegGeneralInfo loadSstNewRegCase(final Long caseId) {
        final RegGeneralInfo regCase = regGeneralInfoRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration case not found: " + caseId));
        assertSstCaseSection(regCase);
        return regCase;
    }

    private Map<String, String> buildAttributes(final RegGeneralInfo regCase, final SalesTaxLetterType letterType) {
        return switch (letterType) {
            case ACKNOWLEDGEMENT -> {
                final SstInfo sstInfo = findPromotedSstInfo(regCase);
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

    /**
     * For an update-tax-payer case, the promoted {@code SstInfo} keeps the {@code regGeneralInfoId}
     * of the original new-registration case (it's updated in place, not re-created) — so it must be
     * looked up by the case's linked employer instead of by the case id itself.
     */
    private SstInfo findPromotedSstInfo(final RegGeneralInfo regCase) {
        if (RegistrationSectionRouting.isUpdateTaxSection(regCase.getSectionId())) {
            final Long employerId = regCase.getEmployerId();
            if (employerId == null) {
                throw new IllegalStateException("Update case has no linked employer: " + regCase.getId());
            }
            return sstInfoRepository.findFirstByEmployerIdAndDeletedFalseOrderByIdDesc(employerId)
                    .orElseThrow(() -> new IllegalStateException(
                            "No active SST info found for updated employer: " + employerId));
        }
        return sstInfoRepository.findByRegGeneralInfoIdAndDeletedFalse(regCase.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Approved SST case has no promoted SST info: " + regCase.getId()));
    }

    private static void assertSstCaseSection(final RegGeneralInfo regCase) {
        if (!RegistrationSectionRouting.isSstCaseSection(regCase.getSectionId())) {
            throw new IllegalArgumentException(
                    "SST letters are only available for SST registration cases (1100-1105, 1200-1204)");
        }
    }

    public record SalesTaxAcknowledgementLetter(String fileName, String contentType, byte[] content) {
    }
}
