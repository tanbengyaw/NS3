package my.gov.perkeso.assist.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class RegistrationApiClient {

    private final String baseUrl;
    private final TestRestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String username;
    private final String password;

    public RegistrationApiClient(final String baseUrl, final TestRestTemplate restTemplate,
            final ObjectMapper objectMapper, final String username, final String password) {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.username = username;
        this.password = password;
    }

    public JsonNode createCase(final String json) {
        return exchangeJson(HttpMethod.POST, "/registration-cases", json);
    }

    public JsonNode updateCase(final long caseId, final String json) {
        return exchangeJson(HttpMethod.PUT, "/registration-cases/" + caseId, json);
    }

    public JsonNode submitCase(final long caseId, final String json) {
        return exchangeJson(HttpMethod.POST, "/registration-cases/" + caseId + "?command=submit", json);
    }

    public JsonNode approveCase(final long caseId) {
        return exchangeJson(HttpMethod.POST, "/registration-cases/" + caseId + "?command=approve", "{}");
    }

    public JsonNode queryCase(final long caseId, final String json) {
        return exchangeJson(HttpMethod.POST, "/registration-cases/" + caseId + "?command=query", json);
    }

    public JsonNode getCaseByRefNo(final String caseRefNo) {
        return exchangeJson(HttpMethod.GET, "/registration-cases/" + caseRefNo, null);
    }

    public JsonNode createEmployee(final long caseId, final String json) {
        return exchangeJson(HttpMethod.POST, "/registration-cases/" + caseId + "/employees", json);
    }

    public JsonNode upsertSstInfo(final long caseId, final String json) {
        return exchangeJson(HttpMethod.PUT, "/registration-cases/" + caseId + "/sst-info", json);
    }

    public JsonNode createDirector(final long caseId, final String json) {
        return exchangeJson(HttpMethod.POST, "/registration-cases/" + caseId + "/sst-info/directors", json);
    }

    public JsonNode createTariffCode(final long caseId, final String json) {
        return exchangeJson(HttpMethod.POST, "/registration-cases/" + caseId + "/sst-info/tariff-codes", json);
    }

    public List<JsonNode> searchTariffCodeSalesTypes(final String search) {
        final ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/reference/tariff-code-sales-types?search=" + search,
                HttpMethod.GET,
                authEntity(null),
                String.class);
        assert2xx(response);
        try {
            return objectMapper.readValue(response.getBody(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, JsonNode.class));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse tariff code search results", ex);
        }
    }

    public List<JsonNode> listEmployees(final long caseId) {
        final ResponseEntity<String> response = restTemplate.exchange(baseUrl + "/registration-cases/" + caseId
                + "/employees", HttpMethod.GET, authEntity(null), String.class);
        assert2xx(response);
        try {
            return objectMapper.readValue(response.getBody(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, JsonNode.class));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse employee list", ex);
        }
    }

    public JsonNode getEmployerByCode(final String employerCode) {
        return exchangeJson(HttpMethod.GET, "/employers/code/" + employerCode, null);
    }

    public JsonNode enrollPortalUser(final String json) {
        return exchangeJson(HttpMethod.POST, "/portal-enrollments", json);
    }

    public JsonNode getPortalUserMe() {
        return exchangeJson(HttpMethod.GET, "/portal-users/me", null);
    }

    private JsonNode exchangeJson(final HttpMethod method, final String path, final String json) {
        final ResponseEntity<String> response = restTemplate.exchange(baseUrl + path, method, authEntity(json),
                String.class);
        assert2xx(response);
        if (response.getBody() == null || response.getBody().isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse response for " + path, ex);
        }
    }

    private HttpEntity<String> authEntity(final String json) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(username, password);
        return new HttpEntity<>(json, headers);
    }

    private static void assert2xx(final ResponseEntity<?> response) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new AssertionError("Expected 2xx but got " + response.getStatusCode() + ": " + response.getBody());
        }
    }
}
