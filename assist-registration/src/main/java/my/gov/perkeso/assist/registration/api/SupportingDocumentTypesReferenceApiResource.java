package my.gov.perkeso.assist.registration.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.data.SupportingDocumentTypeData;
import my.gov.perkeso.assist.registration.service.RegistrationReferenceReadPlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/reference/supporting-document-types")
@Produces({ MediaType.APPLICATION_JSON })
@Tag(name = "Registration Reference", description = "Supporting document type lookup")
@RequiredArgsConstructor
public class SupportingDocumentTypesReferenceApiResource {

    private final RegistrationReferenceReadPlatformService registrationReferenceService;

    @GET
    @Operation(summary = "List supporting document types for sales tax registration")
    public List<SupportingDocumentTypeData> listSupportingDocumentTypesForSalesTax() {
        return registrationReferenceService.retrieveSupportingDocumentTypesForSalesTax();
    }
}
