package my.gov.perkeso.assist.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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

    public List<JsonNode> listCases(final String appStatus) {
        return listCases(appStatus, null, null);
    }

    public List<JsonNode> listCases(final String appStatus, final Long sectionId, final String sectionIds) {
        final StringBuilder path = new StringBuilder("/registration-cases?limit=100");
        if (appStatus != null && !appStatus.isBlank()) {
            path.append("&appStatus=").append(appStatus);
        }
        if (sectionIds != null && !sectionIds.isBlank()) {
            path.append("&sectionIds=").append(sectionIds);
        } else if (sectionId != null) {
            path.append("&sectionId=").append(sectionId);
        }
        final ResponseEntity<String> response = restTemplate.exchange(baseUrl + path, HttpMethod.GET,
                authEntity(null), String.class);
        assert2xx(response);
        try {
            return objectMapper.readValue(response.getBody(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, JsonNode.class));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse registration case list", ex);
        }
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

    public JsonNode getSstInfo(final long caseId) {
        return exchangeJson(HttpMethod.GET, "/registration-cases/" + caseId + "/sst-info", null);
    }

    public JsonNode createDirector(final long caseId, final String json) {
        return exchangeJson(HttpMethod.POST, "/registration-cases/" + caseId + "/sst-info/directors", json);
    }

    public JsonNode createTariffCode(final long caseId, final String json) {
        return exchangeJson(HttpMethod.POST, "/registration-cases/" + caseId + "/sst-info/tariff-codes", json);
    }

    public JsonNode createPremises(final long caseId, final String json) {
        return exchangeJson(HttpMethod.POST, "/registration-cases/" + caseId + "/sst-info/premises", json);
    }

    public JsonNode addServiceCategory(final long caseId, final String json) {
        return exchangeJson(HttpMethod.POST, "/registration-cases/" + caseId + "/sst-info/service-categories", json);
    }

    public JsonNode uploadSupportingDocument(final long caseId, final long documentTypeId, final byte[] content,
            final String fileName, final String contentType) {
        final MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("documentTypeId", documentTypeId);
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });

        final HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        final ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/registration-cases/" + caseId + "/sst-info/supporting-documents",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
        assert2xx(response);
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse supporting document upload response", ex);
        }
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

    public List<JsonNode> searchTaxPayerUpdates(final String taxType, final String search) {
        final ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/reference/tax-payer-updates?taxType=" + taxType + "&search=" + search,
                HttpMethod.GET,
                authEntity(null),
                String.class);
        assert2xx(response);
        try {
            return objectMapper.readValue(response.getBody(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, JsonNode.class));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse tax payer update search results", ex);
        }
    }

    public JsonNode startTaxPayerUpdate(final long employerId, final long sectionId) {
        return exchangeJson(HttpMethod.POST, "/registration-cases/tax-updates", """
                {
                  "employerId": %d,
                  "sectionId": %d
                }
                """.formatted(employerId, sectionId));
    }

    public List<JsonNode> getTaxUpdateDiff(final long caseId) {
        final ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/registration-cases/" + caseId + "/tax-update-diff",
                HttpMethod.GET,
                authEntity(null),
                String.class);
        assert2xx(response);
        try {
            return objectMapper.readValue(response.getBody(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, JsonNode.class));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse tax update diff", ex);
        }
    }

    public List<JsonNode> searchDiscontinueTaxPayers(final String taxType, final String search) {
        final ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/reference/discontinue-tax-payers?taxType=" + taxType + "&search=" + search,
                HttpMethod.GET,
                authEntity(null),
                String.class);
        assert2xx(response);
        try {
            return objectMapper.readValue(response.getBody(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, JsonNode.class));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse discontinue tax payer search results", ex);
        }
    }

    public JsonNode startDiscontinueTax(final long employerId, final long sstInfoId) {
        return exchangeJson(HttpMethod.POST, "/registration-cases/discontinue-tax", """
                {
                  "employerId": %d,
                  "sstInfoId": %d
                }
                """.formatted(employerId, sstInfoId));
    }

    public JsonNode getDiscontinueInfo(final long caseId) {
        return exchangeJson(HttpMethod.GET, "/registration-cases/" + caseId + "/discontinue-info", null);
    }

    public JsonNode upsertDiscontinueInfo(final long caseId, final String json) {
        return exchangeJson(HttpMethod.PUT, "/registration-cases/" + caseId + "/discontinue-info", json);
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

    public ResponseEntity<byte[]> downloadSalesTaxAcknowledgementLetter(final long caseId) {
        return downloadSalesTaxAcknowledgementLetter(caseId, "pdf");
    }

    public ResponseEntity<byte[]> downloadSalesTaxAcknowledgementLetter(final long caseId, final String format) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);
        final ResponseEntity<byte[]> response = restTemplate.exchange(
                baseUrl + "/registration-cases/" + caseId + "/sst-info/acknowledgement-letter?format=" + format,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                byte[].class);
        assert2xx(response);
        return response;
    }

    public JsonNode enrollPortalUser(final String json) {
        return exchangeJson(HttpMethod.POST, "/portal-enrollments", json);
    }

    public JsonNode getPortalUser(final String username) {
        return exchangeJson(HttpMethod.GET, "/portal-users/" + username, null);
    }

    public JsonNode queryPortalEnrollment(final String username, final String json) {
        return exchangeJson(HttpMethod.POST, "/portal-users/" + username + "/query", json);
    }

    public JsonNode approvePortalEnrollment(final String username, final String json) {
        return exchangeJson(HttpMethod.POST, "/portal-users/" + username + "/approve", json);
    }

    public JsonNode rejectPortalEnrollment(final String username) {
        return exchangeJson(HttpMethod.POST, "/portal-users/" + username + "/reject", "{}");
    }

    public JsonNode resubmitPortalEnrollment(final String username, final String json) {
        return exchangeJson(HttpMethod.PUT, "/portal-enrollments/" + username + "/resubmit", json);
    }

    public JsonNode uploadPortalDraftDocument(final String draftToken, final long documentTypeId,
            final byte[] content, final String fileName, final String contentType) {
        final MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("documentTypeId", documentTypeId);
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });

        final HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        final ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/portal-enrollment-drafts/" + draftToken + "/documents",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
        assert2xx(response);
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse portal draft document upload response", ex);
        }
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
