package my.gov.perkeso.assist.registration.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.data.TempEmployeeData;
import my.gov.perkeso.assist.registration.service.TempEmployeeWritePlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/registration-cases/{caseId}/employees")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Tag(name = "Registration Case Employees", description = "Form 2 draft employees for a registration case")
@RequiredArgsConstructor
public class RegistrationCaseEmployeesApiResource {

    private final TempEmployeeWritePlatformService tempEmployeeService;

    @GET
    @Operation(summary = "List draft employees for a registration case")
    public List<TempEmployeeData> listEmployees(@PathParam("caseId") final Long caseId) {
        return tempEmployeeService.listEmployees(caseId);
    }

    @POST
    @Operation(summary = "Add draft employee to registration case")
    public TempEmployeeData createEmployee(@PathParam("caseId") final Long caseId, final String json) {
        return tempEmployeeService.createEmployee(caseId, json);
    }

    @PUT
    @Path("{employeeId}")
    @Operation(summary = "Update draft employee")
    public TempEmployeeData updateEmployee(@PathParam("caseId") final Long caseId,
            @PathParam("employeeId") final Long employeeId, final String json) {
        return tempEmployeeService.updateEmployee(caseId, employeeId, json);
    }

    @DELETE
    @Path("{employeeId}")
    @Operation(summary = "Remove draft employee")
    public Response deleteEmployee(@PathParam("caseId") final Long caseId,
            @PathParam("employeeId") final Long employeeId) {
        tempEmployeeService.deleteEmployee(caseId, employeeId);
        return Response.noContent().build();
    }
}
