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
import my.gov.perkeso.assist.registration.data.SstServiceTypeData;
import my.gov.perkeso.assist.registration.service.RegistrationReferenceReadPlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/reference/sst-service-types")
@Produces({ MediaType.APPLICATION_JSON })
@Tag(name = "Registration Reference", description = "SST service type code lookup")
@RequiredArgsConstructor
public class SstServiceTypesReferenceApiResource {

    private final RegistrationReferenceReadPlatformService registrationReferenceService;

    @GET
    @Operation(summary = "Search SST service types by code prefix or description keyword")
    public List<SstServiceTypeData> searchSstServiceTypes(
            @QueryParam("search") @DefaultValue("") final String search) {
        return registrationReferenceService.searchSstServiceTypes(search);
    }
}
