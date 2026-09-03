package my.gov.perkeso.assist.registration.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.data.TempSstSupportingDocumentData;
import my.gov.perkeso.assist.registration.service.TempSstSupportingDocumentWritePlatformService;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/registration-cases/{caseId}/sst-info/supporting-documents")
@Tag(name = "Registration Case SST Info", description = "Sales tax supporting documents")
@RequiredArgsConstructor
public class RegistrationCaseSstSupportingDocumentsApiResource {

    private final TempSstSupportingDocumentWritePlatformService supportingDocumentService;

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "List draft supporting documents for a sales tax case")
    public List<TempSstSupportingDocumentData> listSupportingDocuments(@PathParam("caseId") final Long caseId) {
        return supportingDocumentService.listSupportingDocuments(caseId);
    }

    @POST
    @Consumes({ MediaType.MULTIPART_FORM_DATA })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Upload a supporting document (PDF, JPG, JPEG, PNG; max 10 MB)")
    public TempSstSupportingDocumentData uploadSupportingDocument(@PathParam("caseId") final Long caseId,
            @FormDataParam("documentTypeId") final Long documentTypeId,
            @FormDataParam("file") final InputStream file,
            @FormDataParam("file") final FormDataContentDisposition fileDetail) {
        try {
            return supportingDocumentService.uploadSupportingDocument(caseId, documentTypeId,
                    fileDetail != null ? fileDetail.getFileName() : null,
                    fileDetail != null ? fileDetail.getType() : null, file);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    @GET
    @Path("{documentId}/content")
    @Operation(summary = "Download a supporting document")
    public Response downloadSupportingDocument(@PathParam("caseId") final Long caseId,
            @PathParam("documentId") final Long documentId) {
        final TempSstSupportingDocumentWritePlatformService.DownloadedRegistrationDocument downloaded =
                supportingDocumentService.downloadSupportingDocument(caseId, documentId);
        return Response.ok(downloaded.resource())
                .header("Content-Disposition", "attachment; filename=\"" + downloaded.fileName() + "\"")
                .type(downloaded.contentType() != null ? downloaded.contentType() : MediaType.APPLICATION_OCTET_STREAM)
                .build();
    }

    @DELETE
    @Path("{documentId}")
    @Operation(summary = "Remove a draft supporting document")
    public Response deleteSupportingDocument(@PathParam("caseId") final Long caseId,
            @PathParam("documentId") final Long documentId) {
        supportingDocumentService.deleteSupportingDocument(caseId, documentId);
        return Response.noContent().build();
    }
}
