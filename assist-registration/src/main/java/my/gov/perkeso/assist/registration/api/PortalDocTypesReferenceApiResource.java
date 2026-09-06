package my.gov.perkeso.assist.registration.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.data.PortalDocTypeData;
import my.gov.perkeso.assist.registration.service.BaseReferenceReadPlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/reference/portal-doc-types")
@Produces({ MediaType.APPLICATION_JSON })
@Tag(name = "Registration Reference", description = "Portal ID document type lookup values")
@RequiredArgsConstructor
public class PortalDocTypesReferenceApiResource {

    private final BaseReferenceReadPlatformService baseReferenceReadPlatformService;

    @GET
    @Operation(summary = "List document types for portal ID registration")
    public List<PortalDocTypeData> retrievePortalDocTypes() {
        return baseReferenceReadPlatformService.retrievePortalDocTypes();
    }
}
