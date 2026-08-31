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
import my.gov.perkeso.assist.registration.data.TariffCodeSalesTypeData;
import my.gov.perkeso.assist.registration.service.RegistrationReferenceReadPlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/reference/tariff-code-sales-types")
@Produces({ MediaType.APPLICATION_JSON })
@Tag(name = "Registration Reference", description = "SST sales tariff code lookup")
@RequiredArgsConstructor
public class TariffCodeSalesTypesReferenceApiResource {

    private final RegistrationReferenceReadPlatformService registrationReferenceService;

    @GET
    @Operation(summary = "Search SST sales tariff codes (13-char code prefix match)")
    public List<TariffCodeSalesTypeData> searchTariffCodeSalesTypes(
            @QueryParam("search") @DefaultValue("") final String search) {
        return registrationReferenceService.searchTariffCodeSalesTypes(search);
    }
}
