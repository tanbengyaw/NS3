package my.gov.perkeso.assist.registration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SalesTaxAcknowledgementLetterTemplateRendererTest {

    private final SalesTaxAcknowledgementLetterTemplateRenderer renderer =
            new SalesTaxAcknowledgementLetterTemplateRenderer();

    @Test
    void render_includesLegacyMalayHeadingAndCss() {
        final String html = renderer.render(acknowledgementAttrs("ABC SDN BHD<br/>47301 PETALING JAYA",
                "Pejabat Shah Alam"));

        assertThat(html).contains("JABATAN KASTAM DIRAJA MALAYSIA");
        assertThat(html).contains("KELULUSAN PENDAFTARAN DI BAWAH SEKSYEN 13 AKTA CUKAI JUALAN 2018");
        assertThat(html).contains("LAMPIRAN I");
        assertThat(html).contains(".titleFontArial11NoBold");
        assertThat(html).contains("CASE-001");
        assertThat(html).contains("KL-CJ-00001234/2024");
        assertThat(html).contains("width=\"70\" height=\"80\"");
        assertThat(html).contains("width=\"65\" height=\"65\"");
        assertThat(html).contains("sst-letter-contact-section");
        assertThat(html).doesNotContain("margin-top: -100px");
        assertThat(html.indexOf("IBU PEJABAT KASTAM DIRAJA MALAYSIA")).isLessThan(html.indexOf("Telefon"));
    }

    @Test
    void renderInquiry_includesAppendix16Heading() {
        final Map<String, String> attrs = new HashMap<>();
        attrs.put("myCSS", "");
        attrs.put("declareHeader", "");
        attrs.put("refNoYour", "CASE-001");
        attrs.put("refNoOur", "208/A37/CASE-001");
        attrs.put("receiveDate", "01/06/2024");
        attrs.put("employerAddressWithName", "<td>ABC SDN BHD</td>");
        attrs.put("employeeList", "");
        attrs.put("orderList", "");
        attrs.put("tagline", "");
        attrs.put("letterPhrase", "");
        attrs.put("idStaff", "Pegawai Pendaftaran");
        attrs.put("branchTitleWithState", "");
        attrs.put("branchName", "");
        attrs.put("isCcNeeded", "");
        attrs.put("moto", "");
        final String html = new SalesTaxAcknowledgementLetterTemplateRenderer().renderInquiry(attrs);

        assertThat(html).contains("PERMINTAAN");
        assertThat(html).contains("KEPERLUAN DOKUMEN");
    }

    @Test
    void normalizeForPdf_escapesBareAmpersandsInDynamicContent() {
        final String html = new SalesTaxAcknowledgementLetterTemplateRenderer()
                .render(acknowledgementAttrs("Foo &amp; Bar SDN BHD<br/>47301 PETALING JAYA",
                        "<span>Pejabat A & B</span>"));

        assertThat(html).doesNotContain("Pejabat A & B");
        assertThat(html).contains("Pejabat A &amp; B");
    }

    private static Map<String, String> acknowledgementAttrs(final String employerAddress,
            final String branchOfficeAddress) {
        final Map<String, String> attrs = new HashMap<>();
        attrs.put("employerAddressWithName", employerAddress);
        attrs.put("refNoOur", "CASE-001");
        attrs.put("date", "15/06/2024");
        attrs.put("refNoYour", "KL-CJ-00001234/2024");
        attrs.put("branchOffice", "Jabatan Kastam Diraja Malaysia Kuala Lumpur");
        attrs.put("requestDate", "01/06/2024");
        attrs.put("approvalDate", "15/06/2024");
        attrs.put("basicAcc", "Asas Bayaran");
        attrs.put("taxPeriod", "Satu Bulan");
        attrs.put("firstTaxPeriod", "01/09/2024 sehingga 30/09/2024");
        attrs.put("lastPaymentDate", "31/10/2024");
        attrs.put("secondTaxPeriod", "01/10/2024 sehingga 30/11/2024");
        attrs.put("lastPaymentDate2", "31/12/2024");
        attrs.put("nextTaxPeriod", "Setiap Dua Bulan");
        attrs.put("lastPaymentDate3", "Hari terakhir bulan berikutnya");
        attrs.put("branchOfficeAddress", branchOfficeAddress);
        attrs.put("nationalEmblem", SalesTaxAcknowledgementLetterAssetResolver.TRANSPARENT_IMAGE_DATA_URI);
        attrs.put("customLogo", SalesTaxAcknowledgementLetterAssetResolver.TRANSPARENT_IMAGE_DATA_URI);
        return attrs;
    }
}
