package my.gov.perkeso.assist.registration.data;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaxPayerRegistrationProfileData {

    private final Long employerId;
    private final String employerCode;
    private final String employerName;
    private final String registrationNo;
    private final Long businessEntityTypeId;
    private final Long msicId;
    private final Long serviceTypeId;
    private final Long pksBranchId;
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
    private final Long methodContributionPaymentId;
    private final String tradeName;
    private final String tourTaxRegNo;
    private final String inTaxRefNo;
    private final String cusAudRefNo;
    private final List<TaxPayerDirectorData> directors;
    private final List<TaxPayerPremisesData> premises;
}
