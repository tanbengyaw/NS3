package my.gov.perkeso.assist.identity.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.identity.data.PortalUserData;
import my.gov.perkeso.assist.identity.service.PortalEnrollmentReadPlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/portal-users")
@Produces({ MediaType.APPLICATION_JSON })
@Tag(name = "Portal Users", description = "Employer portal user profile")
@RequiredArgsConstructor
public class PortalUsersApiResource {

    private final PortalEnrollmentReadPlatformService readService;

    @GET
    @Path("me")
    @Operation(summary = "Get current portal user profile")
    public PortalUserData retrieveCurrentUser() {
        return readService.retrieveCurrentPortalUser();
    }

    @GET
    @Path("{username}")
    @Operation(summary = "Get portal user by username")
    public PortalUserData retrieveByUsername(@PathParam("username") final String username) {
        return readService.retrieveByUsername(username);
    }
}
