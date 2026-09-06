package my.gov.perkeso.assist.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import my.gov.perkeso.assist.ServerApplication;
import my.gov.perkeso.assist.registration.constant.RegistrationSectionRouting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = ServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UoInboxSectionFilterIT {

    private static final String UO_SECTION_IDS = "1103,1200,1201,1202,1203,1204";

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
    void uoInboxFiltersByUoWorkflowSections() {
        final String updateTaxBrn = uniqueBrn();
        final String socsoBrn = uniqueBrn();
        final RegistrationApiClient uo = client("uo_jb", "password");

        final JsonNode updateTaxCase = uo.createCase("""
                {
                  "employerName": "UO Filter Update Tax Co",
                  "registrationNo": "%s",
                  "serviceTypeId": 1,
                  "pksBranchId": 4,
                  "postCode": "80000",
                  "sectionId": 1201,
                  "dataSourceId": 1
                }
                """.formatted(updateTaxBrn));
        final long updateTaxCaseId = updateTaxCase.get("resourceId").asLong();
        uo.updateCase(updateTaxCaseId, """
                {
                  "businessEntityTypeId": 1,
                  "addressLine1": "No 1 Jalan UO Filter"
                }
                """);
        uo.submitCase(updateTaxCaseId, "{}");

        final JsonNode socsoCase = uo.createCase("""
                {
                  "employerName": "UO Filter SOCSO Co",
                  "registrationNo": "%s",
                  "serviceTypeId": 1,
                  "pksBranchId": 4,
                  "postCode": "80000",
                  "sectionId": 200,
                  "dataSourceId": 1
                }
                """.formatted(socsoBrn));
        final long socsoCaseId = socsoCase.get("resourceId").asLong();
        uo.updateCase(socsoCaseId, """
                {
                  "businessEntityTypeId": 1,
                  "addressLine1": "No 2 Jalan SOCSO"
                }
                """);
        uo.submitCase(socsoCaseId, "{}");

        assertThat(uo.listCases("SUBMITTED", null, UO_SECTION_IDS))
                .anySatisfy(item -> assertThat(item.get("id").asLong()).isEqualTo(updateTaxCaseId))
                .noneMatch(item -> item.get("id").asLong() == socsoCaseId);

        assertThat(uo.listCases("SUBMITTED", 1201L, null))
                .anySatisfy(item -> assertThat(item.get("id").asLong()).isEqualTo(updateTaxCaseId));

        assertThat(uo.listCases("SUBMITTED", 200L, null))
                .noneMatch(item -> item.get("id").asLong() == updateTaxCaseId)
                .anySatisfy(item -> assertThat(item.get("id").asLong()).isEqualTo(socsoCaseId));

        assertThat(RegistrationSectionRouting.uoWorkflowSectionIds()).containsExactly(1103L, 1200L, 1201L, 1202L, 1203L,
                1204L);
    }

    private RegistrationApiClient client(final String username, final String password) {
        return new RegistrationApiClient(baseUrl, restTemplate, objectMapper, username, password);
    }

    private static String uniqueBrn() {
        final String suffix = UUID.randomUUID().toString().replaceAll("\\D", "");
        return "2019" + suffix.substring(0, 8);
    }
}
