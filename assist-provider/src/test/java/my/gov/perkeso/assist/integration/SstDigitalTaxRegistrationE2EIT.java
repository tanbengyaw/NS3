package my.gov.perkeso.assist.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import my.gov.perkeso.assist.ServerApplication;
import my.gov.perkeso.assist.registration.constant.TaxType;
import my.gov.perkeso.assist.registration.domain.SstInfoRepository;
import my.gov.perkeso.assist.registration.domain.SstStatusInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = ServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SstDigitalTaxRegistrationE2EIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SstInfoRepository sstInfoRepository;

    @Autowired
    private SstStatusInfoRepository sstStatusInfoRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/assist-provider/api/v1";
    }

    @Test
    void roSubmitAutoApprovesDigitalTaxWithCdSmk() {
        final String brn = uniqueBrn();
        final RegistrationApiClient ro = client("ro", "password");

        final JsonNode created = ro.createCase("""
                {
                  "sectionId": 1102,
                  "employerName": "Global Streaming Sdn Bhd",
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
                  "email": "digital@example.com",
                  "addressLine1": "1 Jalan Digital"
                }
                """);

        ro.upsertSstInfo(caseId, """
                {
                  "tradeName": "Global Streaming",
                  "websiteAddress": "https://streaming.example.com",
                  "finYrEndMon": 12,
                  "businessComDate": "2024-01-01",
                  "dsTypeMusicEbookFilm": true,
                  "dsTypeSearchEngineSocialNetwork": true,
                  "achievingValueOfDsDate": "2024-03-01",
                  "dsTotalValue": 750000.00,
                  "declareTrue": true,
                  "declareDate": "2024-06-15",
                  "applicantName": "Jane Tan"
                }
                """);

        ro.createDirector(caseId, """
                {
                  "name": "Jane Tan",
                  "identificationNo": "P1234567",
                  "telephoneNo": "+60123456789",
                  "designation": "Authorised representative"
                }
                """);

        final byte[] pdfSample = "%PDF-1.4 test".getBytes();
        ro.uploadSupportingDocument(caseId, 2L, pdfSample, "ssm-cert.pdf", "application/pdf");
        ro.uploadSupportingDocument(caseId, 1L, pdfSample, "applicant-id.pdf", "application/pdf");

        final JsonNode approved = ro.submitCase(caseId, "{}");
        assertThat(approved.get("changes").get("appStatus").asText()).isEqualTo("APPROVED");

        final String employerCode = approved.get("resourceIdentifier").asText();
        assertThat(employerCode).contains("-");
        assertThat(approved.get("changes").get("digitalTaxSmkRegNo").asText()).contains("-CD-");
        assertThat(approved.get("changes").get("sstSmkRegNo").asText())
                .isEqualTo(approved.get("changes").get("digitalTaxSmkRegNo").asText());

        final var sstInfoRows = sstInfoRepository.findByRegGeneralInfoIdAndDeletedFalse(caseId);
        assertThat(sstInfoRows).hasSize(1);
        assertThat(sstInfoRows.get(0).getDigitalTaxSmkRegNo()).contains("-CD-");
        assertThat(sstInfoRows.get(0).getSmkRegNo()).isEqualTo(sstInfoRows.get(0).getDigitalTaxSmkRegNo());
        assertThat(sstInfoRows.get(0).getSalesTaxSmkRegNo()).isNull();
        assertThat(sstInfoRows.get(0).isDsTypeMusicEbookFilm()).isTrue();
        assertThat(sstInfoRows.get(0).isDsTypeSearchEngineSocialNetwork()).isTrue();
        assertThat(sstInfoRows.get(0).isDsTypeSoftwareAppsGame()).isFalse();
        assertThat(sstInfoRows.get(0).getDsTotalValue()).isEqualByComparingTo("750000.00");
        assertThat(sstInfoRows.get(0).getFinYrEndMon()).isEqualTo(12);

        final var statuses = sstStatusInfoRepository.findBySstInfoIdAndDeletedFalse(sstInfoRows.get(0).getId());
        assertThat(statuses).isNotEmpty();
        assertThat(statuses.get(0).getTaxTypeId()).isEqualTo(TaxType.DIGITAL_TAX.getAssistId());

        final String digitalSmk = approved.get("changes").get("digitalTaxSmkRegNo").asText();
        final String caseRefNo = approved.get("changes").get("caseRefNo").asText();
        final var htmlLetterResponse = ro.downloadSalesTaxAcknowledgementLetter(caseId, "html");
        final String letterHtml = new String(htmlLetterResponse.getBody());
        assertThat(letterHtml).contains("JABATAN KASTAM DIRAJA MALAYSIA");
        assertThat(letterHtml).contains(caseRefNo);
        assertThat(letterHtml).contains(digitalSmk);
    }

    private RegistrationApiClient client(final String username, final String password) {
        return new RegistrationApiClient(baseUrl, restTemplate, objectMapper, username, password);
    }

    private static String uniqueBrn() {
        return "2019" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
