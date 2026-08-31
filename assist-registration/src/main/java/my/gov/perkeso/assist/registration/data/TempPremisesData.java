package my.gov.perkeso.assist.registration.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TempPremisesData {

    Long id;
    Long caseId;
    String name;
    String addressLine;
    String addressLine2;
    String addressLine3;
    String postCode;
    String cityName;
    String stateName;
}
