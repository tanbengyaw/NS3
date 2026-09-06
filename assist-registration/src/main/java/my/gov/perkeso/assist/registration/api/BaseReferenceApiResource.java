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
import my.gov.perkeso.assist.registration.service.BaseReferenceReadPlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/reference/base")
@Produces({ MediaType.APPLICATION_JSON })
@Tag(name = "Base Reference", description = "Legacy BASE schema reference lookups")
@RequiredArgsConstructor
public class BaseReferenceApiResource {

    private final BaseReferenceReadPlatformService baseReferenceReadPlatformService;

    @GET
    @Path("address-types")
    @Operation(summary = "List base address types (legacy BASE.REF_ADDRESS_TYPE)")
    public List<RefOptionData> retrieveAddressTypes() {
        return baseReferenceReadPlatformService.retrieveAddressTypes();
    }

    @GET
    @Path("contact-types")
    @Operation(summary = "List base contact types (legacy BASE.REF_CONTACT_TYPE)")
    public List<RefOptionData> retrieveContactTypes() {
        return baseReferenceReadPlatformService.retrieveContactTypes();
    }
}
