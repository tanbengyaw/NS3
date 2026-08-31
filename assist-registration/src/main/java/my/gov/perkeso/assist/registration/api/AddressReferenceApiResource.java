package my.gov.perkeso.assist.registration.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.data.PostcodeOptionData;
import my.gov.perkeso.assist.registration.data.RefOptionData;
import my.gov.perkeso.assist.registration.service.AddressReferenceReadPlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/reference/address")
@Produces({ MediaType.APPLICATION_JSON })
@Tag(name = "Address Reference", description = "State, city, and postcode lookup for registration forms")
@RequiredArgsConstructor
public class AddressReferenceApiResource {

    private final AddressReferenceReadPlatformService addressReferenceService;

    @GET
    @Path("states")
    @Operation(summary = "List states for address dropdowns")
    public List<RefOptionData> retrieveStates() {
        return addressReferenceService.retrieveStates();
    }

    @GET
    @Path("cities")
    @Operation(summary = "List cities for a state")
    public List<RefOptionData> retrieveCities(@QueryParam("stateId") final Long stateId) {
        return addressReferenceService.retrieveCities(stateId);
    }

    @GET
    @Path("postcodes")
    @Operation(summary = "List postcodes filtered by state and optional city")
    public List<PostcodeOptionData> retrievePostcodes(@QueryParam("stateId") final Long stateId,
            @QueryParam("cityId") final Long cityId) {
        return addressReferenceService.retrievePostcodes(stateId, cityId);
    }

    @GET
    @Path("office-locations")
    @Operation(summary = "List PKS/SOCSO office locations for a business postcode")
    public List<RefOptionData> retrieveOfficeLocations(@QueryParam("postcode") final String postcode) {
        return addressReferenceService.retrieveOfficeLocations(postcode);
    }
}
