package my.gov.perkeso.assist.registration.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.data.RefOptionData;
import my.gov.perkeso.assist.registration.service.RegistrationReferenceReadPlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/reference/reg-no-types")
@Produces({ MediaType.APPLICATION_JSON })
@Tag(name = "Registration Reference", description = "Registration number type lookup values for portal ID")
@RequiredArgsConstructor
public class RegNoTypesReferenceApiResource {

    private final RegistrationReferenceReadPlatformService registrationReferenceService;

    @GET
    @Operation(summary = "List registration number types (BRN, PTJ, owner ID) for portal ID registration")
    public List<RefOptionData> retrieveRegNoTypes() {
        return registrationReferenceService.retrieveRegNoTypes();
    }
}
