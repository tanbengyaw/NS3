package my.gov.perkeso.assist.identity.data;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PortalUserData {

    private final Long id;
    private final String username;
    private final String email;
    private final String applicationType;
    private final Long employerId;
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
    private final String enrollmentStatus;
    private final String queryRemark;
    private final LocalDateTime enrolledDate;
    private final LocalDateTime linkedDate;
    private final boolean loginActive;
    private final LocalDateTime approvedDate;
}
