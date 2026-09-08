package my.gov.perkeso.assist.registration.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.registration.data.SstNotificationData;
import my.gov.perkeso.assist.registration.data.TaxRegistrantRegistrationInfoData;
import my.gov.perkeso.assist.registration.data.TaxRegistrantSummaryData;
import my.gov.perkeso.assist.registration.service.TaxRegistrantReadPlatformService;
import org.springframework.stereotype.Component;

@Component
@Path("/v1/tax-registrant")
@Produces({ MediaType.APPLICATION_JSON })
@Tag(name = "Tax Registrant 360", description = "Employer portal profile (legacy ASSIST My ASSIST / Tax Registrant 360)")
@RequiredArgsConstructor
public class TaxRegistrantApiResource {

    private final TaxRegistrantReadPlatformService readService;

    @GET
    @Path("me/summary")
    @Operation(summary = "Taxpayer header summary for current portal employer")
    public TaxRegistrantSummaryData retrieveSummary() {
        return readService.retrieveSummaryForCurrentUser();
    }

    @GET
    @Path("me/notifications")
    @Operation(summary = "Notification listing for current portal employer")
    public List<SstNotificationData> listNotifications() {
        return readService.listNotificationsForCurrentUser();
    }

    @GET
    @Path("me/registration-info")
    @Operation(summary = "Registration information (company info + registration types)")
    public TaxRegistrantRegistrationInfoData retrieveRegistrationInfo() {
        return readService.retrieveRegistrationInfoForCurrentUser();
    }
}
