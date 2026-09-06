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
class RoleBasedSubmitRoutingIT {

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
    void roUpdateTaxSectionGoesToSubmittedNotAutoApproved() {
        final String brn = uniqueBrn();
        final RegistrationApiClient ro = client("ro", "password");

        final JsonNode created = ro.createCase("""
                {
                  "employerName": "Update Tax Co",
                  "registrationNo": "%s",
                  "serviceTypeId": 1,
                  "pksBranchId": 2,
                  "postCode": "50812",
                  "sectionId": 1201,
                  "dataSourceId": 1
                }
                """.formatted(brn));
        final long caseId = created.get("resourceId").asLong();

        ro.updateCase(caseId, """
                {
                  "businessEntityTypeId": 1,
                  "addressLine1": "No 1 Jalan Update Tax"
                }
                """);

        final JsonNode submitted = ro.submitCase(caseId, "{}");
        assertThat(submitted.get("changes").get("appStatus").asText()).isEqualTo("SUBMITTED");
    }

    @Test
    void uoUpdateTaxSectionGoesToSubmitted() {
        final String brn = uniqueBrn();
        final RegistrationApiClient uo = client("uo_jb", "password");

        final JsonNode created = uo.createCase("""
                {
                  "employerName": "UO Update Tax Co",
                  "registrationNo": "%s",
                  "serviceTypeId": 1,
                  "pksBranchId": 4,
                  "postCode": "80000",
                  "sectionId": 1201,
                  "dataSourceId": 1
                }
                """.formatted(brn));
        final long caseId = created.get("resourceId").asLong();

        uo.updateCase(caseId, """
                {
                  "businessEntityTypeId": 1,
                  "addressLine1": "No 2 Jalan UO"
                }
                """);

        final JsonNode submitted = uo.submitCase(caseId, "{}");
        assertThat(submitted.get("changes").get("appStatus").asText()).isEqualTo("SUBMITTED");
    }

    @Test
    void pkrBoIncompleteNewRegAutoApproves() {
        final String brn = uniqueBrn();
        final RegistrationApiClient pkrBo = client("pkrbo_kl", "password");

        final JsonNode created = pkrBo.createCase("""
                {
                  "employerName": "PKR Incomplete Co",
                  "registrationNo": "%s",
                  "serviceTypeId": 1,
                  "pksBranchId": 1,
                  "postCode": "50450",
                  "dataSourceId": 1
                }
                """.formatted(brn));
        final long caseId = created.get("resourceId").asLong();

        pkrBo.updateCase(caseId, """
                {
                  "businessEntityTypeId": 1,
                  "addressLine1": "No 3 Jalan PKR"
                }
                """);

        final JsonNode submitted = pkrBo.submitCase(caseId, "{\"incomplete\": true}");
        assertThat(submitted.get("changes").get("appStatus").asText()).isEqualTo("APPROVED");
        assertThat(submitted.get("resourceIdentifier").asText()).isNotBlank();
    }

    @Test
    void officerIncompleteTaxSectionAutoApproves() {
        final String brn = uniqueBrn();
        final RegistrationApiClient officer = client("officer_pj", "password");

        final JsonNode created = officer.createCase("""
                {
                  "employerName": "Incomplete Tax Co",
                  "registrationNo": "%s",
                  "serviceTypeId": 1,
                  "pksBranchId": 3,
                  "postCode": "46000",
                  "sectionId": 1206,
                  "dataSourceId": 1
                }
                """.formatted(brn));
        final long caseId = created.get("resourceId").asLong();

        officer.updateCase(caseId, """
                {
                  "businessEntityTypeId": 1,
                  "addressLine1": "No 4 Jalan Incomplete Tax"
                }
                """);

        final JsonNode submitted = officer.submitCase(caseId, "{}");
        assertThat(submitted.get("changes").get("appStatus").asText()).isEqualTo("APPROVED");
        assertThat(submitted.get("resourceIdentifier").asText()).isNotBlank();
    }

    private RegistrationApiClient client(final String username, final String password) {
        return new RegistrationApiClient(baseUrl, restTemplate, objectMapper, username, password);
    }

    private static String uniqueBrn() {
        final String suffix = UUID.randomUUID().toString().replaceAll("\\D", "");
        return "2019" + suffix.substring(0, 8);
    }
}
