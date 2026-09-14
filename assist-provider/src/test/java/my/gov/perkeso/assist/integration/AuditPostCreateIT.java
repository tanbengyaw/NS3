package my.gov.perkeso.assist.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import my.gov.perkeso.assist.ServerApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = ServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuditPostCreateIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/assist-provider/api/v1";
    }

    @Test
    void officerWalksPlanningFieldworkWorkingPaperAndFindingsToTaxpayerPending() {
        final String brn = uniqueBrn();
        final RegistrationApiClient officer = client("officer_pj", "password");

        final JsonNode draft = officer.createAuditDraft();
        final long caseId = draft.get("resourceId").asLong();
        officer.submitAuditCase(caseId, createCasePayload(brn));

        final JsonNode afterSubmit = officer.getAuditCase(caseId);
        assertThat(afterSubmit.get("taskStatusId").asLong()).isEqualTo(1101L);
        assertThat(afterSubmit.get("planning").get("id").asLong()).isPositive();

        officer.saveAuditPlanning(caseId, """
                {
                  "refProposedCaseTypeId": 2,
                  "timelineFrom": "2026-01-01",
                  "timelineTo": "2026-03-31",
                  "activitiesDetails": "Site visit and sampling",
                  "exclusions": "Related-party loans",
                  "limitationDisclosure": "Records for 2023 incomplete"
                }
                """);

        officer.saveAuditFieldWork(caseId, """
                {
                  "refVisitTypeId": 1,
                  "observation": "Warehouse records sampled",
                  "refSiteVisitOutcomeId": 2,
                  "officerRemark": "Follow up on input tax"
                }
                """);

        final JsonNode createdPaper = officer.createAuditWorkingPaper(caseId);
        final long workingPaperId = createdPaper.get("resourceId").asLong();
        assertThat(createdPaper.get("resourceIdentifier").asText()).startsWith("WP");

        officer.saveAuditWorkingPaper(caseId, workingPaperId, """
                {
                  "refWorkingPaperStatusId": 2,
                  "findingUsage": true,
                  "refFocusAreaId": 1,
                  "proceduresDetail": "Sample sales invoices",
                  "refTestPerformedId": 1,
                  "testDescription": "Recalculate output tax",
                  "populationSize": 100,
                  "populationTotalVal": 500000.00,
                  "populationPeriodFrom": "2025-01-01",
                  "populationPeriodTo": "2025-12-31",
                  "refSamplingMethodId": 1,
                  "sampleCount": 10,
                  "sampleValue": 50000.00,
                  "declaredAmount": 40000.00,
                  "correctAmount": 45000.00,
                  "totalErrorInSample": 5000.00,
                  "errorRate": 10.00,
                  "refProjectionMethodId": 1,
                  "refTaxTypeId": 2,
                  "projectedAdjustment": 50000.00,
                  "applicableTaxRate": 10.00,
                  "additionalTax": 5000.00,
                  "projectedTaxImpact": 5000.00,
                  "refFindingTypeId": 1,
                  "conclution": "Under-declared output tax",
                  "findingDescription": "Invoices omitted from SST-02"
                }
                """);

        final JsonNode submittedFindings = officer.saveAuditFindings(caseId, """
                {
                  "summaryDetail": "Output tax under-declared on sampled invoices.",
                  "submit": true
                }
                """);
        assertThat(submittedFindings.get("changes").get("taskStatusId").asLong()).isEqualTo(1102L);

        final JsonNode insufficient = officer.saveAuditFindings(caseId, """
                {
                  "supervisor": true,
                  "supervisorStatus": 2,
                  "supervisorRemark": "Need more sampling evidence"
                }
                """);
        assertThat(insufficient.get("changes").get("taskStatusId").asLong()).isEqualTo(1101L);

        officer.saveAuditFindings(caseId, """
                {
                  "summaryDetail": "Output tax under-declared; sampling expanded.",
                  "submit": true
                }
                """);
        final JsonNode approved = officer.saveAuditFindings(caseId, """
                {
                  "supervisor": true,
                  "supervisorStatus": 1,
                  "supervisorRemark": "Approved"
                }
                """);
        assertThat(approved.get("changes").get("taskStatusId").asLong()).isEqualTo(1103L);

        final JsonNode loaded = officer.getAuditCase(caseId);
        assertThat(loaded.get("taskStatusId").asLong()).isEqualTo(1103L);
        assertThat(loaded.get("refProposedCaseTypeId").asLong()).isEqualTo(2L);
        assertThat(loaded.get("planning").get("activitiesDetails").asText()).isEqualTo("Site visit and sampling");
        assertThat(loaded.get("fieldWork").get("refVisitTypeId").asLong()).isEqualTo(1L);
        assertThat(loaded.get("workingPapers")).hasSize(1);
        assertThat(loaded.get("workingPapers").get(0).get("findingUsage").asBoolean()).isTrue();
        assertThat(loaded.get("findings").get("supervisorStatus").asInt()).isEqualTo(1);
        assertThat(loaded.get("findings").get("summaryDetail").asText())
                .contains("sampling expanded");
        assertThat(loaded.get("taxpayerResponse").get("responseId").asText()).startsWith("TXR");
        assertThat(loaded.get("taxpayerResponse").get("activated").asBoolean()).isTrue();

        final JsonNode taxpayerSubmitted = officer.saveAuditTaxpayerResponse(caseId, """
                {
                  "responseChannel": 2,
                  "responseType": 1,
                  "taxpayerComments": "We accept the findings.",
                  "submit": true
                }
                """);
        assertThat(taxpayerSubmitted.get("changes").get("taskStatusId").asLong()).isEqualTo(1104L);

        final JsonNode closed = officer.saveAuditTaxpayerResponse(caseId, """
                {
                  "officer": true,
                  "officersFinalOutcome": 1,
                  "officersRemarks": "BOD confirmed"
                }
                """);
        assertThat(closed.get("changes").get("taskStatusId").asLong()).isEqualTo(1107L);

        final JsonNode closedCase = officer.getAuditCase(caseId);
        assertThat(closedCase.get("taskStatusId").asLong()).isEqualTo(1107L);
        assertThat(closedCase.get("taxpayerResponse").get("responseType").asInt()).isEqualTo(1);
        assertThat(closedCase.get("taxpayerResponse").get("officersFinalOutcome").asInt()).isEqualTo(1);
    }

    private RegistrationApiClient client(final String username, final String password) {
        return new RegistrationApiClient(baseUrl, restTemplate, objectMapper, username, password);
    }

    private static String createCasePayload(final String brn) {
        return """
                {
                  "preAuditSkip": true,
                  "refCaseSourceId": 3,
                  "refRiskLevelId": 2,
                  "cusAudRefNo": "AUD-%s",
                  "taxpayer": {
                    "taxPayerName": "Audit Post Create Co",
                    "businessRegNo": "%s",
                    "addressLine1": "Lot 9 Fieldwork Park",
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
                """.formatted(brn.substring(brn.length() - 6), brn);
    }

    private static String uniqueBrn() {
        final String suffix = UUID.randomUUID().toString().replaceAll("\\D", "");
        return "2019" + suffix.substring(0, 8);
    }
}
