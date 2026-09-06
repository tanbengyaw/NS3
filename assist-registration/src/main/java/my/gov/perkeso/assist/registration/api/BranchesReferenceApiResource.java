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
import my.gov.perkeso.assist.registration.service.BranchReferenceReadPlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/reference/branches")
@Produces({ MediaType.APPLICATION_JSON })
@Tag(name = "Branch Reference", description = "PKS branch lookup for staff and registration forms")
@RequiredArgsConstructor
public class BranchesReferenceApiResource {

    private final BranchReferenceReadPlatformService branchReferenceReadPlatformService;

    @GET
    @Operation(summary = "List all PKS branches")
    public List<RefOptionData> retrieveBranches() {
        return branchReferenceReadPlatformService.retrieveAllBranches();
    }
}
