package my.gov.perkeso.assist.registration.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PortalDocTypeData {

    Long id;
    String code;
    String label;
    boolean requiredForPortalId;
}
