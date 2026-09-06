package my.gov.perkeso.assist.identity.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.identity.data.PortalEnrollmentRequest;
import my.gov.perkeso.assist.identity.data.PortalUserData;
import my.gov.perkeso.assist.identity.service.PortalEnrollmentWritePlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/portal-enrollments")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Tag(name = "Portal Enrollments", description = "Employer portal ID enrollment (ASSIST base module)")
@RequiredArgsConstructor
public class PortalEnrollmentsApiResource {

    private final PortalEnrollmentWritePlatformService writeService;
    private final ObjectMapper objectMapper;

    @POST
    @Operation(summary = "Enroll portal user", description = "Full portal ID registration profile")
    public PortalUserData enroll(final String json) {
        return writeService.enroll(parseRequest(json));
    }

    @PUT
    @Path("{username}/resubmit")
    @Operation(summary = "Resubmit portal enrollment after IN_QUERY")
    public PortalUserData resubmit(@PathParam("username") final String username, final String json) {
        return writeService.resubmitEnrollment(username, parseRequest(json));
    }

    private PortalEnrollmentRequest parseRequest(final String json) {
        try {
            return objectMapper.readValue(json, PortalEnrollmentRequest.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JSON payload", ex);
        }
    }
}
