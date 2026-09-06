package my.gov.perkeso.assist.registration.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SstServiceTypeData {
    Long id;
    String code;
    String description;
    boolean accommodation;
}
