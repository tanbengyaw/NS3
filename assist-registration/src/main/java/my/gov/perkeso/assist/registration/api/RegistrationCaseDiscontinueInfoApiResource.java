package my.gov.perkeso.assist.registration.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.data.DiscontinueTaxInfoData;
import my.gov.perkeso.assist.registration.service.DiscontinueTaxCaseService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/registration-cases/{caseId}/discontinue-info")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Tag(name = "Registration Cases", description = "Discontinue Tax draft (new status + cessation-effective date)")
@RequiredArgsConstructor
public class RegistrationCaseDiscontinueInfoApiResource {

    private final DiscontinueTaxCaseService discontinueTaxCaseService;
    private final ObjectMapper objectMapper;

    @GET
    @Operation(summary = "Get Discontinue Tax draft data for a case")
    public DiscontinueTaxInfoData getDiscontinueInfo(@PathParam("caseId") final Long caseId) {
        return discontinueTaxCaseService.getDiscontinueInfo(caseId);
    }

    @PUT
    @Operation(summary = "Set the requested new status + cessation-effective date")
    public DiscontinueTaxInfoData upsertDiscontinueInfo(@PathParam("caseId") final Long caseId, final String json) {
        final JsonNode node = parseJson(json);
        final Long newSstStatusId = longValue(node, "newSstStatusId");
        final LocalDate cessationTaxEffectiveFrom = dateValue(node, "cessationTaxEffectiveFrom");
        return discontinueTaxCaseService.upsertDiscontinueInfo(caseId, newSstStatusId, cessationTaxEffectiveFrom);
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

    private static LocalDate dateValue(final JsonNode node, final String field) {
        if (!node.hasNonNull(field)) {
            return null;
        }
        return LocalDate.parse(node.get(field).asText());
    }
}
