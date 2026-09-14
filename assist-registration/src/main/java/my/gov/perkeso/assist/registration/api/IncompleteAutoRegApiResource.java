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
import my.gov.perkeso.assist.registration.service.IncompleteAutoRegCaseService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/registration-cases/incomplete-auto-regs")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Tag(name = "Registration Cases", description = "Start incomplete auto-reg completion cases (1205-1209)")
@RequiredArgsConstructor
public class IncompleteAutoRegApiResource {

    private final IncompleteAutoRegCaseService incompleteAutoRegCaseService;
    private final ObjectMapper objectMapper;

    @POST
    @Operation(summary = "Start (or resume) an incomplete auto-reg completion case",
            description = "Body: { sstInfoId }")
    public CommandProcessingResult startCompletion(final String json) {
        final JsonNode node = parseJson(json);
        if (!node.hasNonNull("sstInfoId")) {
            throw new IllegalArgumentException("sstInfoId is required");
        }
        return incompleteAutoRegCaseService.startCompletion(node.get("sstInfoId").asLong());
    }

    private JsonNode parseJson(final String json) {
        try {
            return objectMapper.readTree(json);
        } catch (final Exception ex) {
            throw new IllegalArgumentException("Invalid JSON payload", ex);
        }
    }
}
