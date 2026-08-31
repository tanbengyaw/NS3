package my.gov.perkeso.assist.registration.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PostcodeOptionData {

    String postcode;
    String label;
    Long stateId;
    String stateName;
    Long cityId;
    String cityName;
}
