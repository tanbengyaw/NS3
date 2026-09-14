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
import my.gov.perkeso.assist.registration.service.AutoRegTaxPayerIngestService;
import org.springframework.stereotype.Component;

/**
 * Inbound API for audit (or a Phase 1 stub) to plant a partial SST auto-registration.
 * Does not create a 1205–1209 case.
 */
@Component
@Path("/v1/sst-auto-registrations")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Tag(name = "SST Auto Registration", description = "Audit ingest of incomplete auto-registered SST taxpayers")
@RequiredArgsConstructor
public class SstAutoRegistrationApiResource {

    private final AutoRegTaxPayerIngestService ingestService;
    private final ObjectMapper objectMapper;

    @POST
    @Operation(summary = "Ingest an audit auto-registration (partial employer + SstInfo)")
    public CommandProcessingResult ingest(final String json) {
        return ingestService.ingest(parseJson(json));
    }

    private JsonNode parseJson(final String json) {
        try {
            return objectMapper.readTree(json);
        } catch (final Exception ex) {
            throw new IllegalArgumentException("Invalid JSON payload", ex);
        }
    }
}
