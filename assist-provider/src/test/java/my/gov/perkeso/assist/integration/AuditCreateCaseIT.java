package my.gov.perkeso.assist.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import my.gov.perkeso.assist.ServerApplication;
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
class AuditCreateCaseIT {

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
    void officerCreateCaseForUnregisteredBrnPlantsIncompleteAutoReg() {
        final String brn = uniqueBrn();
        final RegistrationApiClient officer = client("officer_pj", "password");

        final JsonNode draft = officer.createAuditDraft();
        final long caseId = draft.get("resourceId").asLong();
        assertThat(draft.get("resourceIdentifier").asText()).startsWith("AUD");

        final JsonNode submitted = officer.submitAuditCase(caseId, """
                {
                  "preAuditSkip": true,
                  "refCaseSourceId": 3,
                  "refRiskLevelId": 2,
                  "cusAudRefNo": "AUD-%s",
                  "taxpayer": {
                    "taxPayerName": "Audit Create Case Co",
                    "businessRegNo": "%s",
                    "addressLine1": "Lot 2 Audit Park",
                    "postcode": "46000",
                    "ns3BranchId": 3
                  },
                  "taxTypes": [
                    {
                      "taxType": "SALES_TAX",
                      "selectedAudit": true,
                      "businessComDate": "2024-01-01",
                      "manSerComDate": "2024-02-01",
                      "finYrEndMon": 12,
                      "annualTtlTaxSalSerVal": 250000.00
                    }
                  ]
                }
                """.formatted(brn.substring(brn.length() - 6), brn));

        assertThat(submitted.get("changes").get("taskStatusId").asLong()).isEqualTo(1101L);
        final long employerId = submitted.get("changes").get("employerId").asLong();
        final long sstInfoId = submitted.get("changes").get("ingested").get(0).get("sstInfoId").asLong();
        assertThat(employerId).isPositive();
        assertThat(sstInfoId).isPositive();

        final SstInfo sstInfo = sstInfoRepository.findById(sstInfoId).orElseThrow();
        assertThat(sstInfo.isAutoRegistration()).isTrue();
        assertThat(sstInfo.getAutoRegistrationSource()).isEqualTo("audit");
        assertThat(sstInfo.getManComDate()).isEqualTo(java.time.LocalDate.parse("2024-02-01"));
        assertThat(sstInfo.getFinYrEndMon()).isEqualTo(12);
        assertThat(sstInfo.getAnTotalTaxSalesVal()).isEqualByComparingTo("250000.00");
        assertThat(sstInfo.getDateSaleValTaxGoods()).isNull();

        final JsonNode loaded = officer.getAuditCase(caseId);
        assertThat(loaded.get("submitted").asBoolean()).isTrue();
        assertThat(loaded.get("taxpayer").get("employerId").asLong()).isEqualTo(employerId);
        assertThat(loaded.get("taxTypes").get(0).get("sstInfoId").asLong()).isEqualTo(sstInfoId);

        final List<JsonNode> listing = officer.searchIncompleteAutoRegs("SALES_TAX", brn);
        assertThat(listing.stream().anyMatch(row -> row.get("sstInfoId").asLong() == sstInfoId)).isTrue();
    }

    private RegistrationApiClient client(final String username, final String password) {
        return new RegistrationApiClient(baseUrl, restTemplate, objectMapper, username, password);
    }

    private static String uniqueBrn() {
        final String suffix = UUID.randomUUID().toString().replaceAll("\\D", "");
        return "2019" + suffix.substring(0, 8);
    }
}
