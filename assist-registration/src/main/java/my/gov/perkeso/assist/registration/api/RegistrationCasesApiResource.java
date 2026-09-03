package my.gov.perkeso.assist.registration.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
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
import my.gov.perkeso.assist.core.commands.domain.CommandWrapper;
import my.gov.perkeso.assist.core.commands.service.CommandProcessingService;
import my.gov.perkeso.assist.core.commands.service.CommandWrapperBuilder;
import my.gov.perkeso.assist.core.infrastructure.data.CommandProcessingResult;
import my.gov.perkeso.assist.registration.data.RegistrationCaseData;
import my.gov.perkeso.assist.registration.data.RegistrationCaseSummaryData;
import my.gov.perkeso.assist.registration.service.EmployerReadPlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/registration-cases")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Tag(name = "Registration Cases", description = "New employer registration draft, submit, approve workflow")
@RequiredArgsConstructor
public class RegistrationCasesApiResource {

    private final CommandProcessingService commandProcessingService;
    private final EmployerReadPlatformService readService;

    @POST
    @Operation(summary = "Create registration case (draft)")
    public CommandProcessingResult create(final String json) {
        final CommandWrapper wrapper = CommandWrapperBuilder.createRegistrationCase().withJson(json).build();
        return commandProcessingService.executeCommand(wrapper);
    }

    @GET
    @Operation(summary = "List registration cases (officer inbox)")
    public List<RegistrationCaseSummaryData> listCases(@QueryParam("appStatus") final String appStatus,
            @QueryParam("sectionId") final Long sectionId,
            @QueryParam("limit") final Integer limit) {
        return readService.retrieveCaseSummaries(appStatus, sectionId, limit != null ? limit : 50);
    }

    @GET
    @Path("id/{caseId}")
    @Operation(summary = "Get registration case by numeric id")
    public RegistrationCaseData retrieveById(@PathParam("caseId") final Long caseId) {
        return readService.retrieveCaseById(caseId);
    }

    @GET
    @Path("{caseRefNo}")
    @Operation(summary = "Get registration case by reference number")
    public RegistrationCaseData retrieveByRefNo(@PathParam("caseRefNo") final String caseRefNo) {
        return readService.retrieveCaseByRefNo(caseRefNo);
    }

    @PUT
    @Path("{caseId}")
    @Operation(summary = "Update registration case draft")
    public CommandProcessingResult update(@PathParam("caseId") final Long caseId, final String json) {
        final CommandWrapper wrapper = CommandWrapperBuilder.updateRegistrationCase(caseId).withJson(json).build();
        return commandProcessingService.executeCommand(wrapper);
    }

    @POST
    @Path("{caseId}")
    @Operation(summary = "Execute case command", description = "command=submit|approve|reject|query")
    public CommandProcessingResult executeCommand(@PathParam("caseId") final Long caseId,
            @QueryParam("command") final String command, final String json) {
        final CommandWrapper wrapper = switch (command != null ? command.toLowerCase() : "") {
            case "submit" -> CommandWrapperBuilder.submitRegistrationCase(caseId).withJson(json).build();
            case "approve" -> CommandWrapperBuilder.approveRegistrationCase(caseId).withJson(json).build();
            case "reject" -> CommandWrapperBuilder.rejectRegistrationCase(caseId).withJson(json).build();
            case "query" -> CommandWrapperBuilder.queryRegistrationCase(caseId).withJson(json).build();
            default -> throw new IllegalArgumentException("Unsupported command: " + command);
        };
        return commandProcessingService.executeCommand(wrapper);
    }
}
