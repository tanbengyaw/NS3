package my.gov.perkeso.assist.identity.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortalEnrollmentRequest {

    private final String username;
    private final String email;
    private final String applicationType;
    private final String employerCode;
    private final String employerName;
    private final Long registrationTypeId;
    private final String registrationNo;
    private final String addressLine1;
    private final String addressLine2;
    private final String addressLine3;
    private final Long stateId;
    private final Long cityId;
    private final String cityName;
    private final String postCode;
    private final String fullName;
    private final Long identificationTypeId;
    private final String identificationNo;
    private final String phoneCallingCode;
    private final String phoneNumber;
    private final String securityPhrase;
    private final String draftToken;
}
