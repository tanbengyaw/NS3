package my.gov.perkeso.assist.identity.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.identity.constant.PortalApplicationType;
import my.gov.perkeso.assist.identity.data.PortalUserData;
import my.gov.perkeso.assist.identity.service.PortalEnrollmentReadPlatformService;
import my.gov.perkeso.assist.identity.service.PortalEnrollmentWritePlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/portal-enrollments")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Tag(name = "Portal Enrollments", description = "Employer portal ID enrollment (ASSIST base module)")
@RequiredArgsConstructor
public class PortalEnrollmentsApiResource {

    private final PortalEnrollmentWritePlatformService writeService;
    private final ObjectMapper objectMapper;

    @POST
    @Operation(summary = "Enroll portal user", description = "applicationType=NEW_EMPLOYER or EXISTING_EMPLOYER")
    public PortalUserData enroll(final String json) {
        final JsonNode node = parseJson(json);
        final String username = requireText(node, "username");
        final String email = requireText(node, "email");
        final PortalApplicationType applicationType = PortalApplicationType.fromValue(requireText(node, "applicationType"));
        final String employerCode = text(node, "employerCode");
        return writeService.enroll(username, email, applicationType, employerCode);
    }

    private JsonNode parseJson(final String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JSON payload", ex);
        }
    }

    private static String requireText(final JsonNode node, final String field) {
        if (!node.hasNonNull(field) || node.get(field).asText().isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return node.get(field).asText();
    }

    private static String text(final JsonNode node, final String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }
}
