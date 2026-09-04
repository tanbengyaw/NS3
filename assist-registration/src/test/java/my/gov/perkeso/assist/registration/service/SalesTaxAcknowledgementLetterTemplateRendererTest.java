package my.gov.perkeso.assist.registration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SalesTaxAcknowledgementLetterTemplateRendererTest {

    private final SalesTaxAcknowledgementLetterTemplateRenderer renderer =
            new SalesTaxAcknowledgementLetterTemplateRenderer();

    @Test
    void render_includesLegacyMalayHeadingAndCss() {
        final String html = renderer.render(Map.of(
                "employerAddressWithName", "ABC SDN BHD<br/>47301 PETALING JAYA",
                "refNoOur", "CASE-001",
                "date", "15/06/2024",
                "refNoYour", "KL-CJ-00001234/2024",
                "branchOffice", "Jabatan Kastam Diraja Malaysia Kuala Lumpur",
                "requestDate", "01/06/2024",
                "approvalDate", "15/06/2024",
                "basicAcc", "Asas Bayaran",
                "taxPeriod", "Satu Bulan",
                "firstTaxPeriod", "01/09/2024 sehingga 30/09/2024",
                "lastPaymentDate", "31/10/2024",
                "secondTaxPeriod", "01/10/2024 sehingga 30/11/2024",
                "lastPaymentDate2", "31/12/2024",
                "nextTaxPeriod", "Setiap Dua Bulan",
                "lastPaymentDate3", "Hari terakhir bulan berikutnya",
                "branchOfficeAddress", "Pejabat Shah Alam",
                "nationalEmblem", SalesTaxAcknowledgementLetterAssetResolver.TRANSPARENT_IMAGE_DATA_URI,
                "customLogo", SalesTaxAcknowledgementLetterAssetResolver.TRANSPARENT_IMAGE_DATA_URI));

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
        final String html = new SalesTaxAcknowledgementLetterTemplateRenderer().renderInquiry(Map.of(
                "myCSS", "",
                "declareHeader", "",
                "refNoYour", "CASE-001",
                "refNoOur", "208/A37/CASE-001",
                "receiveDate", "01/06/2024",
                "employerAddressWithName", "<td>ABC SDN BHD</td>",
                "employeeList", "",
                "orderList", "",
                "tagline", "",
                "letterPhrase", "",
                "idStaff", "Pegawai Pendaftaran",
                "branchTitleWithState", "",
                "branchName", "",
                "isCcNeeded", "",
                "moto", ""));

        assertThat(html).contains("PERMINTAAN");
        assertThat(html).contains("KEPERLUAN DOKUMEN");
    }

    @Test
    void normalizeForPdf_escapesBareAmpersandsInDynamicContent() {
        final String html = new SalesTaxAcknowledgementLetterTemplateRenderer().render(Map.of(
                "employerAddressWithName", "Foo &amp; Bar SDN BHD<br/>47301 PETALING JAYA",
                "refNoOur", "CASE-001",
                "date", "15/06/2024",
                "refNoYour", "KL-CJ-00001234/2024",
                "branchOffice", "Jabatan Kastam Diraja Malaysia Kuala Lumpur",
                "requestDate", "01/06/2024",
                "approvalDate", "15/06/2024",
                "basicAcc", "Asas Bayaran",
                "taxPeriod", "Satu Bulan",
                "firstTaxPeriod", "01/09/2024 sehingga 30/09/2024",
                "lastPaymentDate", "31/10/2024",
                "secondTaxPeriod", "01/10/2024 sehingga 30/11/2024",
                "lastPaymentDate2", "31/12/2024",
                "nextTaxPeriod", "Setiap Dua Bulan",
                "lastPaymentDate3", "Hari terakhir bulan berikutnya",
                "branchOfficeAddress", "<span>Pejabat A & B</span>",
                "nationalEmblem", SalesTaxAcknowledgementLetterAssetResolver.TRANSPARENT_IMAGE_DATA_URI,
                "customLogo", SalesTaxAcknowledgementLetterAssetResolver.TRANSPARENT_IMAGE_DATA_URI));

        assertThat(html).doesNotContain("Pejabat A & B");
        assertThat(html).contains("Pejabat A &amp; B");
    }
}
