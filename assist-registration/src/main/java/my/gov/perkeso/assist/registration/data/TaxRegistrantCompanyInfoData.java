package my.gov.perkeso.assist.registration.data;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaxRegistrantCompanyInfoData {

    private final String businessType;
    private final String businessRegistrationNo;
    private final String registeredBusinessName;
    private final String tradeName;
    private final String premiseAddressLine1;
    private final String premiseAddressLine2;
    private final String premiseAddressLine3;
    private final String telNo;
}
