package my.gov.perkeso.assist.registration.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.data.TaxPayerRegistrationProfileData;
import my.gov.perkeso.assist.registration.service.TaxPayerSearchReadPlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/registration/tax-payer-search")
@Produces({ MediaType.APPLICATION_JSON })
@Tag(name = "Registration", description = "Search existing tax payers for new registration")
@RequiredArgsConstructor
public class TaxPayerSearchApiResource {

    private final TaxPayerSearchReadPlatformService taxPayerSearchService;

    @GET
    @Operation(summary = "Search existing tax payer for SST registration auto-populate")
    public TaxPayerRegistrationProfileData searchTaxPayer(
            @QueryParam("searchType") @DefaultValue("SST_REGISTRATION_NO") final String searchType,
            @QueryParam("searchValue") final String searchValue) {
        return taxPayerSearchService.retrieveForRegistration(searchType, searchValue);
    }
}
