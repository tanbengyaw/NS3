package my.gov.perkeso.assist.registration.data;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegistrationCaseData {

    private final Long id;
    private final String caseRefNo;
    private final String appStatus;
    private final String appStatusReason;
    private final Long sectionId;
    private final String sectionCode;
    private final Long dataSourceId;
    private final String dataSourceCode;
    private final Long casePksBranchId;
    private final Long processingPksBranchId;
    private final Long receivingPksBranchId;
    private final String employerName;
    private final String registrationNo;
    private final Long businessEntityTypeId;
    private final String email;
    private final String phone;
    private final String contactPhones;
    private final String contactFaxes;
    private final String addressLine1;
    private final String addressLine2;
    private final String addressLine3;
    private final Long stateId;
    private final Long cityId;
    private final String cityName;
    private final String postCode;
    private final String corrAddressLine1;
    private final String corrAddressLine2;
    private final String corrAddressLine3;
    private final String corrPostCode;
    private final Long corrStateId;
    private final Long corrCityId;
    private final String corrCityName;
    private final Long serviceTypeId;
    private final Long pksBranchId;
    private final boolean branch;
    private final Long msicId;
    private final Long methodContributionPaymentId;
    private final Long employerId;
    private final String employerCode;
    private final String salesTaxSmkRegNo;
    private final String createdByUsername;
    private final String submittedByUsername;
    private final LocalDateTime documentReceivedDate;
    private final LocalDateTime submissionDate;
    private final LocalDateTime createdDate;
    private final String inqueryByUsername;
    private final LocalDateTime inqueryDate;
    private final String queryRemark;
}
