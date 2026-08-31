package my.gov.perkeso.assist.registration.data;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaxPayerPremisesData {

    private final String name;
    private final String addressLine;
    private final String addressLine2;
    private final String addressLine3;
    private final String postCode;
    private final String cityName;
    private final String stateName;
}
