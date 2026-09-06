package my.gov.perkeso.assist.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import my.gov.perkeso.assist.ServerApplication;
import my.gov.perkeso.assist.registration.constant.SstContractType;
import my.gov.perkeso.assist.registration.domain.SstInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = ServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SstSalesTaxRegistrationE2EIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SstInfoRepository sstInfoRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/assist-provider/api/v1";
    }

    @Test
    void searchTariffCodeSalesTypesByPrefix() {
        final RegistrationApiClient ro = client("ro", "password");
        final List<JsonNode> matches = ro.searchTariffCodeSalesTypes("01011");
        assertThat(matches).isNotEmpty();
        assertThat(matches.get(0).get("code").asText()).startsWith("01011");
        assertThat(matches.get(0).get("description").asText()).isNotBlank();
    }

    @Test
    void roSubmitAutoApprovesSalesTaxWithSmkAndSstInfo() {
        final String brn = uniqueBrn();
        final RegistrationApiClient ro = client("ro", "password");

        final JsonNode created = ro.createCase("""
                {
                  "sectionId": 1100,
                  "employerName": "Sales Tax Manufacturing Sdn Bhd",
                  "registrationNo": "%s",
                  "serviceTypeId": 1,
                  "pksBranchId": 2,
                  "postCode": "50812",
                  "dataSourceId": 1
                }
                """.formatted(brn));
        final long caseId = created.get("resourceId").asLong();

        ro.updateCase(caseId, """
                {
                  "businessEntityTypeId": 1,
                  "email": "sales@example.com",
                  "addressLine1": "Lot 1 Industrial Park"
                }
                """);

        ro.upsertSstInfo(caseId, """
                {
                  "tradeName": "Sales Tax Manufacturing",
                  "anTotalTaxSalesVal": 1500000.00,
                  "dateSaleValTaxGoods": "2024-06-01",
                  "manComDate": "2024-01-15",
                  "finYrEndMon": 12,
                  "businessComDate": "2024-01-01",
                  "localSales": 1000000.00,
                  "exportSales": 300000.00,
                  "salesToDesignArea": 100000.00,
                  "othersSales": 100000.00,
                  "subContractWork": false,
                  "declareTrue": true,
                  "declareDate": "2024-06-15",
                  "applicantName": "Tan Ah Kow"
                }
                """);

        ro.createDirector(caseId, """
                {
                  "name": "Tan Ah Kow",
                  "identificationTypeId": 2,
                  "identificationNo": "800101015432",
                  "email": "director@example.com",
                  "designation": "Managing Director"
                }
                """);

        ro.createTariffCode(caseId, """
                {
                  "tariffCodeSalesTypeId": 101,
                  "contractTypeId": %d,
                  "finishedGoods": "Finished plastic goods"
                }
                """.formatted(SstContractType.MAIN_CONTRACT.getAssistId()));

        final byte[] pdfSample = "%PDF-1.4 test".getBytes();
        ro.uploadSupportingDocument(caseId, 2L, pdfSample, "ssm-cert.pdf", "application/pdf");
        ro.uploadSupportingDocument(caseId, 1L, pdfSample, "applicant-id.pdf", "application/pdf");

        final JsonNode approved = ro.submitCase(caseId, "{}");
        assertThat(approved.get("changes").get("appStatus").asText()).isEqualTo("APPROVED");

        final String employerCode = approved.get("resourceIdentifier").asText();
        assertThat(employerCode).contains("-");
        assertThat(approved.get("changes").get("salesTaxSmkRegNo").asText()).contains("-CJ-");

        final var sstInfoRows = sstInfoRepository.findByRegGeneralInfoIdAndDeletedFalse(caseId);
        assertThat(sstInfoRows).hasSize(1);
        assertThat(sstInfoRows.get(0).getSalesTaxSmkRegNo()).contains("-CJ-");
        assertThat(sstInfoRows.get(0).getSmkRegNo()).isEqualTo(sstInfoRows.get(0).getSalesTaxSmkRegNo());
        assertThat(sstInfoRows.get(0).getAnTotalTaxSalesVal()).isEqualByComparingTo("1500000.00");

        final String salesTaxSmkRegNo = approved.get("changes").get("salesTaxSmkRegNo").asText();
        final var letterResponse = ro.downloadSalesTaxAcknowledgementLetter(caseId);
        assertThat(letterResponse.getHeaders().getContentType()).isNotNull();
        assertThat(new String(letterResponse.getBody(), 0, 4)).isEqualTo("%PDF");
        final var htmlLetterResponse = ro.downloadSalesTaxAcknowledgementLetter(caseId, "html");
        final String letterHtml = new String(htmlLetterResponse.getBody());
        assertThat(letterHtml).contains("KELULUSAN PENDAFTARAN DI BAWAH SEKSYEN 13 AKTA CUKAI JUALAN 2018");
        assertThat(letterHtml).contains("JABATAN KASTAM DIRAJA MALAYSIA");
        assertThat(letterHtml).contains(employerCode);
        assertThat(letterHtml).contains(salesTaxSmkRegNo);
        assertThat(letterHtml).contains("Tan Ah Kow");
    }

    private RegistrationApiClient client(final String username, final String password) {
        return new RegistrationApiClient(baseUrl, restTemplate, objectMapper, username, password);
    }

    private static String uniqueBrn() {
        return "2019" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
