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
import my.gov.perkeso.assist.registration.service.TaxPayerUpdateCaseService;
import org.springframework.stereotype.Component;

/**
 * Starts an "Update Tax Payer" case (sections 1200-1204): clones a live employer's current
 * Employer/SstInfo/children into a fresh temp draft, which the existing SST new-reg wizard route
 * then edits by caseId exactly as it would a reopened draft.
 */
@Component
@Path("/v1/registration-cases/tax-updates")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Tag(name = "Registration Cases", description = "Update Tax Payer case start (clone live employer -> temp draft)")
@RequiredArgsConstructor
public class TaxPayerUpdateApiResource {

    private final TaxPayerUpdateCaseService taxPayerUpdateCaseService;
    private final ObjectMapper objectMapper;

    @POST
    @Operation(summary = "Start an Update Tax Payer case", description = "Body: { employerId, sectionId }")
    public CommandProcessingResult startUpdate(final String json) {
        final JsonNode node = parseJson(json);
        final Long employerId = longValue(node, "employerId");
        final Long sectionId = longValue(node, "sectionId");
        if (employerId == null) {
            throw new IllegalArgumentException("employerId is required");
        }
        if (sectionId == null) {
            throw new IllegalArgumentException("sectionId is required");
        }
        return taxPayerUpdateCaseService.startUpdate(employerId, sectionId);
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
