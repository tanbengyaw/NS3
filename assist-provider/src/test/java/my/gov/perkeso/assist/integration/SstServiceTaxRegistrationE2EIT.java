package my.gov.perkeso.assist.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import my.gov.perkeso.assist.ServerApplication;
import my.gov.perkeso.assist.registration.constant.TaxType;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfo;
import my.gov.perkeso.assist.registration.domain.RegGeneralInfoRepository;
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
class SstServiceTaxRegistrationE2EIT {

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

    @Autowired
    private RegGeneralInfoRepository regGeneralInfoRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/assist-provider/api/v1";
    }

    @Test
    void roSubmitAutoApprovesServiceTaxWithCpSmk() {
        final String brn = uniqueBrn();
        final RegistrationApiClient ro = client("ro", "password");

        final JsonNode created = ro.createCase("""
                {
                  "sectionId": 1105,
                  "employerName": "Service Provider Sdn Bhd",
                  "registrationNo": "%s",
                  "businessEntityTypeId": 2,
                  "serviceTypeId": 1,
                  "pksBranchId": 2,
                  "postCode": "50812",
                  "dataSourceId": 1
                }
                """.formatted(brn));
        final long caseId = created.get("resourceId").asLong();

        ro.updateCase(caseId, """
                {
                  "email": "service@example.com",
                  "addressLine1": "10 Jalan Servis"
                }
                """);

        ro.upsertSstInfo(caseId, """
                {
                  "tradeName": "Service Provider",
                  "inTaxRefNo": "IT-999",
                  "manComDate": "2023-06-01",
                  "dateSaleValTaxGoods": "2023-07-01",
                  "finYrEndMon": 12,
                  "businessComDate": "2023-01-01",
                  "anTotalTaxSalesVal": 500000.00,
                  "declareTrue": true,
                  "declareDate": "2024-06-15",
                  "applicantName": "Siti Aminah"
                }
                """);

        ro.createDirector(caseId, """
                {
                  "name": "Siti Aminah",
                  "identificationNo": "901230-12-1234",
                  "designation": "CEO"
                }
                """);

        ro.addServiceCategory(caseId, """
                {
                  "sstServiceTypeId": 1,
                  "remark": "Main service"
                }
                """);

        final byte[] pdfSample = "%PDF-1.4 test".getBytes();
        ro.uploadSupportingDocument(caseId, 2L, pdfSample, "ssm-cert.pdf", "application/pdf");
        ro.uploadSupportingDocument(caseId, 1L, pdfSample, "applicant-id.pdf", "application/pdf");

        final JsonNode approved = ro.submitCase(caseId, "{}");
        assertThat(approved.get("changes").get("appStatus").asText()).isEqualTo("APPROVED");

        final String employerCode = approved.get("resourceIdentifier").asText();
        assertThat(employerCode).contains("-");
        assertThat(approved.get("changes").get("serviceTaxSmkRegNo").asText()).contains("-CP-");
        assertThat(approved.get("changes").get("sstSmkRegNo").asText())
                .isEqualTo(approved.get("changes").get("serviceTaxSmkRegNo").asText());

        final var sstInfoRows = sstInfoRepository.findByRegGeneralInfoIdAndDeletedFalse(caseId);
        assertThat(sstInfoRows).hasSize(1);
        assertThat(sstInfoRows.get(0).getServiceTaxSmkRegNo()).contains("-CP-");
        assertThat(sstInfoRows.get(0).getSmkRegNo()).isEqualTo(sstInfoRows.get(0).getServiceTaxSmkRegNo());
        assertThat(sstInfoRows.get(0).getSalesTaxSmkRegNo()).isNull();
        assertThat(sstInfoRows.get(0).getManComDate().toString()).isEqualTo("2023-06-01");
        assertThat(sstInfoRows.get(0).getFinYrEndMon()).isEqualTo(12);

        final var statuses = sstStatusInfoRepository.findBySstInfoIdAndDeletedFalse(sstInfoRows.get(0).getId());
        assertThat(statuses).isNotEmpty();
        assertThat(statuses.get(0).getTaxTypeId()).isEqualTo(TaxType.SERVICE_TAX.getAssistId());

        final String serviceSmk = approved.get("changes").get("serviceTaxSmkRegNo").asText();
        final String caseRefNo = approved.get("changes").get("caseRefNo").asText();
        final var htmlLetterResponse = ro.downloadSalesTaxAcknowledgementLetter(caseId, "html");
        final String letterHtml = new String(htmlLetterResponse.getBody());
        assertThat(letterHtml).contains("JABATAN KASTAM DIRAJA MALAYSIA");
        assertThat(letterHtml).contains(caseRefNo);
        assertThat(letterHtml).contains(serviceSmk);
    }

    @Test
    void accommodationServiceOverLimitAutoTriggersTourismTaxCase() {
        final String brn = uniqueBrn();
        final RegistrationApiClient ro = client("ro", "password");

        final JsonNode created = ro.createCase("""
                {
                  "sectionId": 1105,
                  "employerName": "Hotel Warisan Sdn Bhd",
                  "registrationNo": "%s",
                  "businessEntityTypeId": 2,
                  "serviceTypeId": 1,
                  "pksBranchId": 2,
                  "postCode": "50812",
                  "dataSourceId": 1
                }
                """.formatted(brn));
        final long caseId = created.get("resourceId").asLong();

        // Below the RM 500,000 tourism-tax trigger limit — should not trigger yet.
        ro.upsertSstInfo(caseId, """
                {
                  "tradeName": "Hotel Warisan",
                  "anTotalTaxSalesVal": 100000.00,
                  "declareTrue": false
                }
                """);
        ro.addServiceCategory(caseId, """
                {
                  "sstServiceTypeId": 16,
                  "remark": "Accommodation"
                }
                """);

        final JsonNode belowLimit = ro.getSstInfo(caseId);
        assertThat(belowLimit.get("tourismTaxTriggered").asBoolean()).isFalse();

        // Crossing the RM 500,000 limit with an accommodation service type must auto-create a
        // linked Tourism Tax new-registration case (mirrors ASSIST NewSstRegCounter).
        final JsonNode afterUpsert = ro.upsertSstInfo(caseId, """
                {
                  "anTotalTaxSalesVal": 6700000.00
                }
                """);

        assertThat(afterUpsert.get("tourismTaxTriggered").asBoolean()).isTrue();
        final long tourismCaseId = afterUpsert.get("tourismTaxCaseId").asLong();
        final String tourismCaseRefNo = afterUpsert.get("tourismTaxCaseRefNo").asText();
        assertThat(tourismCaseRefNo).isNotBlank();

        final RegGeneralInfo tourismCase = regGeneralInfoRepository.findById(tourismCaseId).orElseThrow();
        assertThat(tourismCase.getSectionId()).isEqualTo(1101L);
        assertThat(tourismCase.getCaseRefNo()).isEqualTo(tourismCaseRefNo);
        assertThat(tourismCase.getTempEmployer().getEmployerName()).isEqualTo("Hotel Warisan Sdn Bhd");
        assertThat(tourismCase.getTempEmployer().getBusinessInfo().getRegistrationNo()).isEqualTo(brn);

        // The auto-created tourism case must link back to the originating service tax case
        // so the UI can navigate from the tourism wizard back to it.
        assertThat(tourismCase.getLinkedCaseId()).isEqualTo(caseId);
        final RegGeneralInfo serviceCase = regGeneralInfoRepository.findById(caseId).orElseThrow();
        assertThat(tourismCase.getLinkedCaseRefNo()).isEqualTo(serviceCase.getCaseRefNo());

        // Re-saving Form 2 again must not create a second tourism case (idempotent trigger).
        final JsonNode afterSecondUpsert = ro.upsertSstInfo(caseId, """
                {
                  "anTotalTaxSalesVal": 7000000.00
                }
                """);
        assertThat(afterSecondUpsert.get("tourismTaxCaseId").asLong()).isEqualTo(tourismCaseId);
    }

    private RegistrationApiClient client(final String username, final String password) {
        return new RegistrationApiClient(baseUrl, restTemplate, objectMapper, username, password);
    }

    private static String uniqueBrn() {
        return "2019" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
