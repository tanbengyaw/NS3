package my.gov.perkeso.assist.audit.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.audit.data.AuditCaseDetailData;
import my.gov.perkeso.assist.audit.data.AuditCaseListingData;
import my.gov.perkeso.assist.audit.service.AuditCaseReadPlatformService;
import my.gov.perkeso.assist.audit.service.AuditCaseWritePlatformService;
import my.gov.perkeso.assist.core.infrastructure.data.CommandProcessingResult;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/audit-cases")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Tag(name = "Audit Cases", description = "Create Case slice — plants incomplete auto-reg on submit")
@RequiredArgsConstructor
public class AuditCasesApiResource {

    private final AuditCaseWritePlatformService writeService;
    private final AuditCaseReadPlatformService readService;
    private final ObjectMapper objectMapper;

    @GET
    @Operation(summary = "List audit cases")
    public List<AuditCaseListingData> search(@QueryParam("search") @DefaultValue("") final String search) {
        return readService.search(search);
    }

    @POST
    @Operation(summary = "Create a draft audit case")
    public CommandProcessingResult createDraft() {
        return writeService.createDraft();
    }

    @GET
    @Path("{caseId}")
    @Operation(summary = "Get an audit case")
    public AuditCaseDetailData get(@PathParam("caseId") final Long caseId) {
        return readService.get(caseId);
    }

    @PUT
    @Path("{caseId}")
    @Operation(summary = "Save draft audit case, taxpayer, and tax-type fields")
    public CommandProcessingResult save(@PathParam("caseId") final Long caseId, final String json) {
        return writeService.save(caseId, parseJson(json));
    }

    @POST
    @Path("{caseId}")
    @Operation(summary = "Submit / create the audit case (ingest unregistered taxpayer)")
    public CommandProcessingResult submit(@PathParam("caseId") final Long caseId,
            @QueryParam("command") final String command, final String json) {
        if (command == null || command.isBlank() || "submit".equalsIgnoreCase(command)
                || "create".equalsIgnoreCase(command)) {
            return writeService.submit(caseId, parseJson(json));
        }
        throw new IllegalArgumentException("Unsupported command: " + command);
    }

    private JsonNode parseJson(final String json) {
        if (json == null || json.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(json);
        } catch (final Exception ex) {
            throw new IllegalArgumentException("Invalid JSON payload", ex);
        }
    }
}
