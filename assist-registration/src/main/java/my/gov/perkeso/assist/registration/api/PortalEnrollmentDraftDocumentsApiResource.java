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
import my.gov.perkeso.assist.registration.data.PortalEnrollmentDraftDocumentData;
import my.gov.perkeso.assist.registration.service.PortalEnrollmentDraftDocumentWritePlatformService;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/portal-enrollment-drafts/{draftToken}/documents")
@Tag(name = "Portal Enrollments", description = "Draft documents for portal ID registration")
@RequiredArgsConstructor
public class PortalEnrollmentDraftDocumentsApiResource {

    private final PortalEnrollmentDraftDocumentWritePlatformService draftDocumentService;

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "List draft documents for a portal enrollment")
    public List<PortalEnrollmentDraftDocumentData> listDraftDocuments(@PathParam("draftToken") final String draftToken) {
        return draftDocumentService.listDraftDocuments(draftToken);
    }

    @POST
    @Consumes({ MediaType.MULTIPART_FORM_DATA })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Upload a draft portal document (PDF, JPG, JPEG, PNG; max 10 MB)")
    public PortalEnrollmentDraftDocumentData uploadDraftDocument(@PathParam("draftToken") final String draftToken,
            @FormDataParam("documentTypeId") final Long documentTypeId,
            @FormDataParam("file") final InputStream file,
            @FormDataParam("file") final FormDataContentDisposition fileDetail) {
        try {
            return draftDocumentService.uploadDraftDocument(draftToken, documentTypeId,
                    fileDetail != null ? fileDetail.getFileName() : null,
                    fileDetail != null ? fileDetail.getType() : null, file);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    @GET
    @Path("{documentId}/content")
    @Operation(summary = "Download a draft portal document")
    public Response downloadDraftDocument(@PathParam("draftToken") final String draftToken,
            @PathParam("documentId") final Long documentId) {
        final PortalEnrollmentDraftDocumentWritePlatformService.DownloadedBaseDocument downloaded =
                draftDocumentService.downloadDraftDocument(draftToken, documentId);
        return Response.ok(downloaded.resource())
                .header("Content-Disposition", "attachment; filename=\"" + downloaded.fileName() + "\"")
                .type(downloaded.contentType() != null ? downloaded.contentType() : MediaType.APPLICATION_OCTET_STREAM)
                .build();
    }

    @DELETE
    @Path("{documentId}")
    @Operation(summary = "Remove a draft portal document")
    public Response deleteDraftDocument(@PathParam("draftToken") final String draftToken,
            @PathParam("documentId") final Long documentId) {
        draftDocumentService.deleteDraftDocument(draftToken, documentId);
        return Response.noContent().build();
    }
}
