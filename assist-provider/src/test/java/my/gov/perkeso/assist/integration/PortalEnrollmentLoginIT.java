package my.gov.perkeso.assist.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import my.gov.perkeso.assist.ServerApplication;
import my.gov.perkeso.assist.registration.domain.PortalUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = ServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PortalEnrollmentLoginIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PortalUserRepository portalUserRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/assist-provider/api/v1";
    }

    @Test
    void approvedPortalUserCanLoginAndLoadProfile() {
        final RegistrationApiClient admin = client("admin", "password");
        final String username = "portal_login_" + UUID.randomUUID().toString().substring(0, 8);
        final String draftToken = UUID.randomUUID().toString();
        uploadRequiredPortalDocuments(admin, draftToken);

        admin.enrollPortalUser("""
                {
                  "username": "%s",
                  "email": "%s@example.com",
                  "applicationType": "NEW_EMPLOYER",
                  "employerName": "Portal Login Co",
                  "registrationTypeId": 1,
                  "registrationNo": "201901019998",
                  "addressLine1": "No 1 Jalan Portal",
                  "stateId": 14,
                  "cityId": 1401,
                  "cityName": "Kuala Lumpur",
                  "postCode": "50450",
                  "fullName": "Portal Login User",
                  "identificationTypeId": 2,
                  "identificationNo": "900101011299",
                  "phoneCallingCode": "+60",
                  "phoneNumber": "123456789",
                  "securityPhrase": "My secret phrase",
                  "draftToken": "%s"
                }
                """.formatted(username, username, draftToken));

        final JsonNode approved = admin.approvePortalEnrollment(username, """
                { "password": "PortalPass1" }
                """);
        assertThat(approved.get("enrollmentStatus").asText()).isEqualTo("APPROVED");
        assertThat(approved.get("loginActive").asBoolean()).isTrue();

        final var stored = portalUserRepository.findByUsernameIgnoreCase(username);
        assertThat(stored).isPresent();
        assertThat(stored.get().isLoginEnabled()).isTrue();

        final RegistrationApiClient portalClient = client(username, "PortalPass1");
        final JsonNode profile = portalClient.getPortalUserMe();
        assertThat(profile.get("username").asText()).isEqualTo(username);
        assertThat(profile.get("enrollmentStatus").asText()).isEqualTo("APPROVED");
    }

    @Test
    void submittedPortalUserCannotLoginBeforeApproval() {
        final RegistrationApiClient admin = client("admin", "password");
        final String username = "portal_pending_" + UUID.randomUUID().toString().substring(0, 8);
        final String draftToken = UUID.randomUUID().toString();
        uploadRequiredPortalDocuments(admin, draftToken);

        admin.enrollPortalUser("""
                {
                  "username": "%s",
                  "email": "%s@example.com",
                  "applicationType": "NEW_EMPLOYER",
                  "employerName": "Pending Co",
                  "registrationTypeId": 1,
                  "registrationNo": "201901019997",
                  "addressLine1": "No 2 Jalan Portal",
                  "stateId": 14,
                  "cityId": 1401,
                  "cityName": "Kuala Lumpur",
                  "postCode": "50450",
                  "fullName": "Pending User",
                  "identificationTypeId": 2,
                  "identificationNo": "900101011298",
                  "phoneCallingCode": "+60",
                  "phoneNumber": "123456788",
                  "securityPhrase": "Pending phrase",
                  "draftToken": "%s"
                }
                """.formatted(username, username, draftToken));

        final RegistrationApiClient pendingClient = client(username, "AnyPassword1");
        try {
            pendingClient.getPortalUserMe();
            throw new AssertionError("Expected unauthorized before approval");
        } catch (AssertionError expected) {
            assertThat(expected.getMessage()).contains("Expected 2xx");
        }
    }

    private void uploadRequiredPortalDocuments(final RegistrationApiClient admin, final String draftToken) {
        admin.uploadPortalDraftDocument(draftToken, 1L, "dummy".getBytes(), "id.pdf", "application/pdf");
        admin.uploadPortalDraftDocument(draftToken, 2L, "dummy".getBytes(), "support.pdf", "application/pdf");
    }

    private RegistrationApiClient client(final String username, final String password) {
        return new RegistrationApiClient(baseUrl, restTemplate, objectMapper, username, password);
    }
}
