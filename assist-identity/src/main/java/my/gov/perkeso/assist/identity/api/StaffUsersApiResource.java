package my.gov.perkeso.assist.identity.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.identity.data.CreateStaffUserRequest;
import my.gov.perkeso.assist.identity.data.StaffUserData;
import my.gov.perkeso.assist.identity.data.UpdateStaffUserRequest;
import my.gov.perkeso.assist.identity.service.StaffUserReadPlatformService;
import my.gov.perkeso.assist.identity.service.StaffUserWritePlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/staff-users")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Tag(name = "Staff Users", description = "PERKESO staff account administration")
@RequiredArgsConstructor
public class StaffUsersApiResource {

    private final StaffUserReadPlatformService readService;
    private final StaffUserWritePlatformService writeService;
    private final ObjectMapper objectMapper;

    @GET
    @Operation(summary = "List staff users", description = "ADMIN only")
    public List<StaffUserData> listStaffUsers() {
        return readService.retrieveAll();
    }

    @GET
    @Path("me")
    @Operation(summary = "Get the current authenticated user's own staff profile",
            description = "Any authenticated staff user — backs the frontend's post-login role fetch")
    public StaffUserData retrieveCurrent() {
        return readService.retrieveCurrent();
    }

    @GET
    @Path("{staffUserId}")
    @Operation(summary = "Get staff user by id", description = "ADMIN only")
    public StaffUserData retrieveById(@PathParam("staffUserId") final Long staffUserId) {
        return readService.retrieveById(staffUserId);
    }

    @POST
    @Operation(summary = "Create staff user", description = "ADMIN only")
    public StaffUserData createStaffUser(final String json) {
        return writeService.createStaffUser(parseCreateRequest(json));
    }

    @PUT
    @Path("{staffUserId}")
    @Operation(summary = "Update staff user", description = "ADMIN only — roles, branch, active flag, password")
    public StaffUserData updateStaffUser(@PathParam("staffUserId") final Long staffUserId, final String json) {
        return writeService.updateStaffUser(staffUserId, parseUpdateRequest(json));
    }

    private CreateStaffUserRequest parseCreateRequest(final String json) {
        try {
            return objectMapper.readValue(json, CreateStaffUserRequest.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JSON payload", ex);
        }
    }

    private UpdateStaffUserRequest parseUpdateRequest(final String json) {
        try {
            return objectMapper.readValue(json, UpdateStaffUserRequest.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JSON payload", ex);
        }
    }
}
