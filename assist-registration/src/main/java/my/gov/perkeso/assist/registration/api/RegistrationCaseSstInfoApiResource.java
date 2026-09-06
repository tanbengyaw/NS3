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
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.data.TempDirectorOwnerData;
import my.gov.perkeso.assist.registration.data.TempPremisesData;
import my.gov.perkeso.assist.registration.data.TempSstContactPersonData;
import my.gov.perkeso.assist.registration.data.TempSstInfoData;
import my.gov.perkeso.assist.registration.data.TempSstServiceCategoryData;
import my.gov.perkeso.assist.registration.data.TempSstTariffCodeData;
import my.gov.perkeso.assist.registration.service.TempSstInfoWritePlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/registration-cases/{caseId}/sst-info")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Tag(name = "Registration Case SST Info", description = "SST new-reg draft data (sections 1100, 1101, 1102, 1104)")
@RequiredArgsConstructor
public class RegistrationCaseSstInfoApiResource {

    private final TempSstInfoWritePlatformService tempSstInfoService;

    @GET
    @Operation(summary = "Get SST draft data for an SST new registration case")
    public TempSstInfoData getSstInfo(@PathParam("caseId") final Long caseId) {
        return tempSstInfoService.getSstInfo(caseId);
    }

    @PUT
    @Operation(summary = "Create or update SST Form 1/2 draft data")
    public TempSstInfoData upsertSstInfo(@PathParam("caseId") final Long caseId, final String json) {
        return tempSstInfoService.upsertSstInfo(caseId, json);
    }

    @POST
    @Path("directors")
    @Operation(summary = "Add draft director (mandatory for sales tax submit)")
    public TempDirectorOwnerData createDirector(@PathParam("caseId") final Long caseId, final String json) {
        return tempSstInfoService.createDirector(caseId, json);
    }

    @PUT
    @Path("directors/{directorId}")
    @Operation(summary = "Update draft director")
    public TempDirectorOwnerData updateDirector(@PathParam("caseId") final Long caseId,
            @PathParam("directorId") final Long directorId, final String json) {
        return tempSstInfoService.updateDirector(caseId, directorId, json);
    }

    @DELETE
    @Path("directors/{directorId}")
    @Operation(summary = "Remove draft director")
    public Response deleteDirector(@PathParam("caseId") final Long caseId,
            @PathParam("directorId") final Long directorId) {
        tempSstInfoService.deleteDirector(caseId, directorId);
        return Response.noContent().build();
    }

    @POST
    @Path("premises")
    @Operation(summary = "Add draft premises (required for tourism tax submit)")
    public TempPremisesData createPremises(@PathParam("caseId") final Long caseId, final String json) {
        return tempSstInfoService.createPremises(caseId, json);
    }

    @PUT
    @Path("premises/{premisesId}")
    @Operation(summary = "Update draft premises")
    public TempPremisesData updatePremises(@PathParam("caseId") final Long caseId,
            @PathParam("premisesId") final Long premisesId, final String json) {
        return tempSstInfoService.updatePremises(caseId, premisesId, json);
    }

    @DELETE
    @Path("premises/{premisesId}")
    @Operation(summary = "Remove draft premises")
    public Response deletePremises(@PathParam("caseId") final Long caseId,
            @PathParam("premisesId") final Long premisesId) {
        tempSstInfoService.deletePremises(caseId, premisesId);
        return Response.noContent().build();
    }

    @POST
    @Path("tariff-codes")
    @Operation(summary = "Add draft tariff code (main or sub contract)")
    public TempSstTariffCodeData createTariffCode(@PathParam("caseId") final Long caseId, final String json) {
        return tempSstInfoService.createTariffCode(caseId, json);
    }

    @PUT
    @Path("tariff-codes/{tariffId}")
    @Operation(summary = "Update draft tariff code")
    public TempSstTariffCodeData updateTariffCode(@PathParam("caseId") final Long caseId,
            @PathParam("tariffId") final Long tariffId, final String json) {
        return tempSstInfoService.updateTariffCode(caseId, tariffId, json);
    }

    @DELETE
    @Path("tariff-codes/{tariffId}")
    @Operation(summary = "Remove draft tariff code")
    public Response deleteTariffCode(@PathParam("caseId") final Long caseId, @PathParam("tariffId") final Long tariffId) {
        tempSstInfoService.deleteTariffCode(caseId, tariffId);
        return Response.noContent().build();
    }

    @POST
    @Path("service-categories")
    @Operation(summary = "Add draft SST service type code (required for service tax submit)")
    public TempSstServiceCategoryData createServiceCategory(@PathParam("caseId") final Long caseId,
            final String json) {
        return tempSstInfoService.createServiceCategory(caseId, json);
    }

    @DELETE
    @Path("service-categories/{categoryId}")
    @Operation(summary = "Remove draft SST service type code")
    public Response deleteServiceCategory(@PathParam("caseId") final Long caseId,
            @PathParam("categoryId") final Long categoryId) {
        tempSstInfoService.deleteServiceCategory(caseId, categoryId);
        return Response.noContent().build();
    }

    @POST
    @Path("contact-persons")
    @Operation(summary = "Add draft SST contact person (Form 2)")
    public TempSstContactPersonData createContactPerson(@PathParam("caseId") final Long caseId, final String json) {
        return tempSstInfoService.createContactPerson(caseId, json);
    }

    @PUT
    @Path("contact-persons/{contactPersonId}")
    @Operation(summary = "Update draft SST contact person")
    public TempSstContactPersonData updateContactPerson(@PathParam("caseId") final Long caseId,
            @PathParam("contactPersonId") final Long contactPersonId, final String json) {
        return tempSstInfoService.updateContactPerson(caseId, contactPersonId, json);
    }

    @DELETE
    @Path("contact-persons/{contactPersonId}")
    @Operation(summary = "Remove draft SST contact person")
    public Response deleteContactPerson(@PathParam("caseId") final Long caseId,
            @PathParam("contactPersonId") final Long contactPersonId) {
        tempSstInfoService.deleteContactPerson(caseId, contactPersonId);
        return Response.noContent().build();
    }
}
