package my.gov.perkeso.assist.registration.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.infrastructure.data.CommandProcessingResult;
import my.gov.perkeso.assist.registration.service.DiscontinueTaxCaseService;
import org.springframework.stereotype.Component;

/**
 * Starts a "Discontinue Tax" case (section 1103): targets a specific (employer, sstInfo) pair and
 * lets the caller pick a new status + cessation-effective date before submitting for UO approval.
 */
@Component
@Path("/v1/registration-cases/discontinue-tax")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Tag(name = "Registration Cases", description = "Discontinue Tax case start")
@RequiredArgsConstructor
public class DiscontinueTaxApiResource {

    private final DiscontinueTaxCaseService discontinueTaxCaseService;
    private final ObjectMapper objectMapper;

    @POST
    @Operation(summary = "Start a Discontinue Tax case", description = "Body: { employerId, sstInfoId }")
    public CommandProcessingResult startDiscontinue(final String json) {
        final JsonNode node = parseJson(json);
        final Long employerId = longValue(node, "employerId");
        final Long sstInfoId = longValue(node, "sstInfoId");
        if (employerId == null) {
            throw new IllegalArgumentException("employerId is required");
        }
        if (sstInfoId == null) {
            throw new IllegalArgumentException("sstInfoId is required");
        }
        return discontinueTaxCaseService.startDiscontinue(employerId, sstInfoId);
    }

    private JsonNode parseJson(final String json) {
        try {
            return objectMapper.readTree(json);
        } catch (final Exception ex) {
            throw new IllegalArgumentException("Invalid JSON payload", ex);
        }
    }

    private static Long longValue(final JsonNode node, final String field) {
        if (!node.hasNonNull(field)) {
            return null;
        }
        return node.get(field).asLong();
    }
}
