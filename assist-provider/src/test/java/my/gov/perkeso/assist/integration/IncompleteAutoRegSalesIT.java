package my.gov.perkeso.assist.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import my.gov.perkeso.assist.ServerApplication;
import my.gov.perkeso.assist.registration.constant.SstContractType;
import my.gov.perkeso.assist.registration.domain.SstInfo;
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
class IncompleteAutoRegSalesIT {

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
    void ingestListsThenOfficerCompletesAndAutoApprovesInPlace() {
        final String brn = uniqueBrn();
        final RegistrationApiClient officer = client("officer_pj", "password");

        final JsonNode ingested = officer.ingestSstAutoRegistration("""
                {
                  "taxType": "SALES_TAX",
                  "employerName": "Audit Incomplete Manufacturing",
                  "registrationNo": "%s",
                  "postCode": "46000",
                  "pksBranchId": 3,
                  "addressLine1": "Lot 9 Audit Park",
                  "businessComDate": "2024-01-01",
                  "finYrEndMon": 12,
                  "anTotalTaxSalesVal": 250000.00,
                  "cusAudRefNo": "AUD-%s",
                  "source": "audit"
                }
                """.formatted(brn, brn.substring(brn.length() - 6)));
        final long sstInfoId = ingested.get("resourceId").asLong();
        final long employerId = ingested.get("changes").get("employerId").asLong();
        assertThat(ingested.get("changes").get("sectionId").asLong()).isEqualTo(1206L);

        final SstInfo afterIngest = sstInfoRepository.findById(sstInfoId).orElseThrow();
        assertThat(afterIngest.isAutoRegistration()).isTrue();
        assertThat(afterIngest.getSalesTaxSmkRegNo()).isBlank();
        assertThat(afterIngest.getManComDate()).isNull();
        assertThat(afterIngest.getDateSaleValTaxGoods()).isNull();

        final List<JsonNode> listing = officer.searchIncompleteAutoRegs("SALES_TAX", brn);
        assertThat(listing).isNotEmpty();
        assertThat(listing.stream().anyMatch(row -> row.get("sstInfoId").asLong() == sstInfoId)).isTrue();

        final JsonNode started = officer.startIncompleteAutoReg(sstInfoId);
        final long caseId = started.get("resourceId").asLong();

        officer.updateCase(caseId, """
                {
                  "businessEntityTypeId": 1,
                  "addressLine1": "Lot 9 Audit Park"
                }
                """);
        officer.upsertSstInfo(caseId, """
                {
                  "tradeName": "Audit Incomplete Manufacturing",
                  "anTotalTaxSalesVal": 250000.00,
                  "dateSaleValTaxGoods": "2024-06-01",
                  "manComDate": "2024-01-15",
                  "finYrEndMon": 12,
                  "businessComDate": "2024-01-01",
                  "localSales": 250000.00,
                  "exportSales": 0,
                  "salesToDesignArea": 0,
                  "othersSales": 0,
                  "subContractWork": false,
                  "declareTrue": true,
                  "declareDate": "2024-06-15",
                  "applicantName": "Audit Officer"
                }
                """);
        officer.createDirector(caseId, """
                {
                  "name": "Audit Officer",
                  "identificationTypeId": 2,
                  "identificationNo": "800101015432",
                  "designation": "Director"
                }
                """);
        officer.createTariffCode(caseId, """
                {
                  "tariffCodeSalesTypeId": 101,
                  "contractTypeId": %d,
                  "finishedGoods": "Audit goods"
                }
                """.formatted(SstContractType.MAIN_CONTRACT.getAssistId()));

        final JsonNode submitted = officer.submitCase(caseId, "{}");
        assertThat(submitted.get("changes").get("appStatus").asText()).isEqualTo("APPROVED");
        assertThat(submitted.get("changes").get("sstInfoId").asLong()).isEqualTo(sstInfoId);
        assertThat(submitted.get("changes").get("salesTaxSmkRegNo").asText()).isNotBlank();
        assertThat(submitted.get("resourceId").asLong()).isEqualTo(employerId);

        final SstInfo afterApprove = sstInfoRepository.findById(sstInfoId).orElseThrow();
        assertThat(afterApprove.isAutoRegistration()).isTrue();
        assertThat(afterApprove.getSalesTaxSmkRegNo()).isNotBlank();
        assertThat(afterApprove.getManComDate()).isNotNull();
        assertThat(afterApprove.getDateSaleValTaxGoods()).isNotNull();

        final List<JsonNode> listingAfter = officer.searchIncompleteAutoRegs("SALES_TAX", brn);
        assertThat(listingAfter.stream().noneMatch(row -> row.get("sstInfoId").asLong() == sstInfoId)).isTrue();
    }

    private RegistrationApiClient client(final String username, final String password) {
        return new RegistrationApiClient(baseUrl, restTemplate, objectMapper, username, password);
    }

    private static String uniqueBrn() {
        final String suffix = UUID.randomUUID().toString().replaceAll("\\D", "");
        return "2019" + suffix.substring(0, 8);
    }
}
