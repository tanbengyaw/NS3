package my.gov.perkeso.assist.registration.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.infrastructure.service.Page;
import my.gov.perkeso.assist.core.infrastructure.service.SearchParameters;
import my.gov.perkeso.assist.registration.data.EmployerData;
import my.gov.perkeso.assist.registration.service.EmployerReadPlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/employers")
@Produces({ MediaType.APPLICATION_JSON })
@Tag(name = "Employers", description = "Registered employer search and profile")
@RequiredArgsConstructor
public class EmployersApiResource {

    private final EmployerReadPlatformService readService;

    @GET
    @Operation(summary = "List employers", description = "Search by name, code, or BRN (registration number)")
    public Page<EmployerData> retrieveAll(@QueryParam("searchType") final String searchType,
            @QueryParam("searchValue") final String searchValue, @QueryParam("officeId") final Long officeId,
            @QueryParam("offset") @DefaultValue("0") final Integer offset,
            @QueryParam("limit") @DefaultValue("20") final Integer limit) {
        return readService.retrieveAll(
                SearchParameters.forEmployers(searchType, searchValue, officeId, offset, limit));
    }

    @GET
    @Path("code/{employerCode}")
    @Operation(summary = "Get employer by code")
    public EmployerData retrieveByCode(@PathParam("employerCode") final String employerCode) {
        return readService.retrieveByEmployerCode(employerCode);
    }

    @GET
    @Path("{employerId}")
    @Operation(summary = "Get employer by id")
    public EmployerData retrieveOne(@PathParam("employerId") final Long employerId) {
        return readService.retrieveOne(employerId);
    }
}
