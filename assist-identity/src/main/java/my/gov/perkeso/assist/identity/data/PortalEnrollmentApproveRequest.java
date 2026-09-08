package my.gov.perkeso.assist.identity.data;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class PortalEnrollmentApproveRequest {

    private final String password;
}
