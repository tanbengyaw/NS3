package my.gov.perkeso.assist.registration.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.service.SalesTaxAcknowledgementLetterFormat;
import my.gov.perkeso.assist.registration.service.SalesTaxAcknowledgementLetterService;
import my.gov.perkeso.assist.registration.service.SalesTaxLetterType;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/registration-cases/{caseId}/sst-info/acknowledgement-letter")
@Tag(name = "Registration Case SST Info", description = "Sales tax acknowledgement letter")
@RequiredArgsConstructor
public class RegistrationCaseSalesTaxLetterApiResource {

    private final SalesTaxAcknowledgementLetterService acknowledgementLetterService;

    @GET
    @Operation(summary = "Download sales tax registration letter (PDF or HTML)",
            description = "letterType=acknowledgement (default), inquiry, or rejection; format=pdf (default) or html")
    public Response downloadLetter(@PathParam("caseId") final Long caseId,
            @QueryParam("letterType") final String letterType,
            @QueryParam("format") final String format) {
        final SalesTaxLetterType resolvedLetterType = SalesTaxLetterType.fromQueryParam(letterType);
        final SalesTaxAcknowledgementLetterFormat letterFormat = SalesTaxAcknowledgementLetterFormat
                .fromQueryParam(format);
        final SalesTaxAcknowledgementLetterService.SalesTaxAcknowledgementLetter letter =
                acknowledgementLetterService.generateLetter(caseId, resolvedLetterType, letterFormat);
        return Response.ok(letter.content())
                .header("Content-Disposition", "attachment; filename=\"" + letter.fileName() + "\"")
                .type(letter.contentType())
                .build();
    }
}
