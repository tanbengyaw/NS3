package my.gov.perkeso.assist.identity.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.identity.data.PortalEnrollmentQueryRequest;
import my.gov.perkeso.assist.identity.data.PortalUserData;
import my.gov.perkeso.assist.identity.service.PortalEnrollmentReadPlatformService;
import my.gov.perkeso.assist.identity.service.PortalEnrollmentWritePlatformService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/portal-users")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Tag(name = "Portal Users", description = "Employer portal user profile")
@RequiredArgsConstructor
public class PortalUsersApiResource {

    private final PortalEnrollmentReadPlatformService readService;
    private final PortalEnrollmentWritePlatformService writeService;
    private final ObjectMapper objectMapper;

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

    @POST
    @Path("{username}/query")
    @Operation(summary = "Send portal enrollment to IN_QUERY")
    public PortalUserData queryEnrollment(@PathParam("username") final String username, final String json) {
        return writeService.queryEnrollment(username, parseQueryRequest(json).getRemark());
    }

    private PortalEnrollmentQueryRequest parseQueryRequest(final String json) {
        try {
            return objectMapper.readValue(json, PortalEnrollmentQueryRequest.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JSON payload", ex);
        }
    }
}
