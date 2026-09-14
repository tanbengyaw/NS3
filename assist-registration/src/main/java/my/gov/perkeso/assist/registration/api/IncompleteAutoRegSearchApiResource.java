package my.gov.perkeso.assist.registration.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.data.IncompleteAutoRegSearchResultData;
import my.gov.perkeso.assist.registration.service.IncompleteAutoRegSearchReadPlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/reference/incomplete-auto-regs")
@Produces({ MediaType.APPLICATION_JSON })
@Tag(name = "Registration Reference", description = "Search incomplete audit auto-reg SST records")
@RequiredArgsConstructor
public class IncompleteAutoRegSearchApiResource {

    private final IncompleteAutoRegSearchReadPlatformService searchService;

    @GET
    @Operation(summary = "List incomplete auto-registration SST rows for staff completion")
    public List<IncompleteAutoRegSearchResultData> search(@QueryParam("taxType") final String taxType,
            @QueryParam("search") @DefaultValue("") final String search) {
        return searchService.search(taxType, search);
    }
}
