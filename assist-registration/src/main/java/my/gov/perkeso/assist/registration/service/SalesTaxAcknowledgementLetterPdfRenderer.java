package my.gov.perkeso.assist.registration.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import org.springframework.stereotype.Component;

@Component
public class SalesTaxAcknowledgementLetterPdfRenderer {

    byte[] renderPdf(final String html, final String baseUri) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            final PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, baseUri);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate PDF acknowledgement letter", ex);
        }
    }
}
