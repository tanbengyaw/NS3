package my.gov.perkeso.assist.registration.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
public class SalesTaxAcknowledgementLetterTemplateRenderer {

    private static final String TEMPLATE_PATH = "templates/legacy/sst_sales_tax_reg_ack_letter.html";
    private static final String CSS_PATH = "templates/legacy/registrationCSS.css";

    String render(final Map<String, String> attributes) {
        String html = loadResource(TEMPLATE_PATH);
        final String css = loadResource(CSS_PATH);
        html = html.replace("$myCSS$", css);
        for (final Map.Entry<String, String> entry : attributes.entrySet()) {
            html = html.replace("$" + entry.getKey() + "$", entry.getValue() != null ? entry.getValue() : "");
        }
        return normalizeForPdf(html);
    }

    private static String normalizeForPdf(final String html) {
        final String withBreaks = html.replaceAll("(?i)<br\\s*>", "<br/>")
                .replaceAll("(?i)<br\\s*/\\s*>", "<br/>");
        return escapeBareAmpersands(withBreaks);
    }

    /**
     * openhtmltopdf parses HTML as XHTML; bare {@code &} (e.g. "Foo & Bar") must be {@code &amp;}.
     */
    private static String escapeBareAmpersands(final String html) {
        return html.replaceAll("&(?!(amp|lt|gt|quot|nbsp|#\\d+|#x[0-9a-fA-F]+);)", "&amp;");
    }

    String templateBaseUri() {
        try {
            return new ClassPathResource("templates/legacy/").getURL().toExternalForm();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to resolve legacy letter template base URI", ex);
        }
    }

    private static String loadResource(final String path) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return StreamUtils.copyToString(input, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load letter resource: " + path, ex);
        }
    }
}
