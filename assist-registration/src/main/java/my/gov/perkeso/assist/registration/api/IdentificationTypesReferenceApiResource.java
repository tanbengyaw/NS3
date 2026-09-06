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
import my.gov.perkeso.assist.registration.data.RefOptionData;
import my.gov.perkeso.assist.registration.service.RegistrationReferenceReadPlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/reference/identification-types")
@Produces({ MediaType.APPLICATION_JSON })
@Tag(name = "Registration Reference", description = "Identification type lookup values")
@RequiredArgsConstructor
public class IdentificationTypesReferenceApiResource {

    private final RegistrationReferenceReadPlatformService registrationReferenceService;

    @GET
    @Operation(summary = "List identification types for registration forms")
    public List<RefOptionData> retrieveIdentificationTypes(
            @QueryParam("directorFormOnly") @DefaultValue("true") final boolean directorFormOnly,
            @QueryParam("portalFormOnly") @DefaultValue("false") final boolean portalFormOnly) {
        return registrationReferenceService.retrieveIdentificationTypes(directorFormOnly, portalFormOnly);
    }
}
