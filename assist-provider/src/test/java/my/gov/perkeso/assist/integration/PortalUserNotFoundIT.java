package my.gov.perkeso.assist.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = ServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PortalUserNotFoundIT {

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
    void unknownPortalUserReturns404WithJsonBody() throws Exception {
        final HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("admin", "password");
        final ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/portal-users/does-not-exist-" + System.nanoTime(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        final JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("defaultUserMessage").asText()).contains("Portal user not found");
        assertThat(body.get("userMessageGlobalisationCode").asText()).isEqualTo("error.msg.resource.not.found");
    }
}
