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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = ServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class StaffUsersApiIT {

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
    void adminCanCreateStaffUser() throws Exception {
        final String suffix = UUID.randomUUID().toString().replaceAll("\\D", "").substring(0, 8);
        final String username = "ro_" + suffix;
        final String email = username + "@perkeso.example";

        final ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/staff-users",
                HttpMethod.POST,
                authEntity("""
                        {
                          "username": "%s",
                          "email": "%s",
                          "password": "password",
                          "branchId": 2,
                          "roles": ["RO"],
                          "active": true
                        }
                        """.formatted(username, email)),
                String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        final JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("username").asText()).isEqualTo(username);
        assertThat(body.get("roles").get(0).asText()).isEqualTo("RO");
        assertThat(body.get("id").asLong()).isGreaterThan(6L);
    }

    private HttpEntity<String> authEntity(final String json) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth("admin", "password");
        return new HttpEntity<>(json, headers);
    }
}
