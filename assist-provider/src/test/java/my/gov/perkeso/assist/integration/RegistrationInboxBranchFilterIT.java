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
class RegistrationInboxBranchFilterIT {

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
    void inboxListsOnlyCasesForStaffBranch() {
        final String brn = uniqueBrn();
        final RegistrationApiClient officerPj = client("officer_pj", "password");
        final RegistrationApiClient roShahAlam = client("ro", "password");
        final RegistrationApiClient admin = client("admin", "password");

        final JsonNode created = officerPj.createCase("""
                {
                  "employerName": "PJ Branch Co",
                  "registrationNo": "%s",
                  "serviceTypeId": 1,
                  "pksBranchId": 3,
                  "postCode": "50812",
                  "dataSourceId": 1
                }
                """.formatted(brn));
        final long caseId = created.get("resourceId").asLong();

        officerPj.updateCase(caseId, """
                {
                  "businessEntityTypeId": 1,
                  "email": "pj@example.com",
                  "addressLine1": "No 1 Jalan PJ"
                }
                """);
        officerPj.submitCase(caseId, "{}");

        assertThat(officerPj.listCases("SUBMITTED")).anySatisfy(item -> assertThat(item.get("id").asLong())
                .isEqualTo(caseId));
        assertThat(officerPj.listCases("SUBMITTED")).allSatisfy(item -> assertThat(item.get("processingPksBranchId")
                .asLong()).isEqualTo(3L));

        assertThat(roShahAlam.listCases("SUBMITTED")).noneMatch(item -> item.get("id").asLong() == caseId);

        assertThat(admin.listCases("SUBMITTED")).anySatisfy(item -> assertThat(item.get("id").asLong())
                .isEqualTo(caseId));
    }

    private RegistrationApiClient client(final String username, final String password) {
        return new RegistrationApiClient(baseUrl, restTemplate, objectMapper, username, password);
    }

    private static String uniqueBrn() {
        final String suffix = UUID.randomUUID().toString().replaceAll("\\D", "");
        return "2019" + suffix.substring(0, 8);
    }
}
