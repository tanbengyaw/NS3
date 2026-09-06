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
import my.gov.perkeso.assist.registration.data.TaxPayerUpdateSearchResultData;
import my.gov.perkeso.assist.registration.service.TaxPayerUpdateSearchReadPlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/reference/tax-payer-updates")
@Produces({ MediaType.APPLICATION_JSON })
@Tag(name = "Registration Reference", description = "Search existing active taxpayers for the Update Tax Payer workflow")
@RequiredArgsConstructor
public class TaxPayerUpdateSearchApiResource {

    private final TaxPayerUpdateSearchReadPlatformService searchService;

    @GET
    @Operation(summary = "Search existing taxpayers active for a given tax type, for starting an update case")
    public List<TaxPayerUpdateSearchResultData> search(@QueryParam("taxType") final String taxType,
            @QueryParam("search") @DefaultValue("") final String search) {
        return searchService.search(taxType, search);
    }
}
