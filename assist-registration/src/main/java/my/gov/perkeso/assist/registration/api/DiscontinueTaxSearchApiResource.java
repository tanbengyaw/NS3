package my.gov.perkeso.assist.registration.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.data.DiscontinueTaxSearchResultData;
import my.gov.perkeso.assist.registration.service.DiscontinueTaxSearchReadPlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/reference/discontinue-tax-payers")
@Produces({ MediaType.APPLICATION_JSON })
@Tag(name = "Registration Reference", description = "Search currently-active taxpayers for the Discontinue Tax workflow")
@RequiredArgsConstructor
public class DiscontinueTaxSearchApiResource {

    private final DiscontinueTaxSearchReadPlatformService searchService;

    @GET
    @Operation(summary = "Search active taxpayers for a given tax type, for starting a discontinue case")
    public List<DiscontinueTaxSearchResultData> search(@QueryParam("taxType") final String taxType,
            @QueryParam("search") @DefaultValue("") final String search) {
        return searchService.search(taxType, search);
    }
}
