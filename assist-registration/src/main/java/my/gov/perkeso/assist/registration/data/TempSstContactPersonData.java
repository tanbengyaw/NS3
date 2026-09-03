package my.gov.perkeso.assist.registration.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TempSstContactPersonData {

    Long id;
    String name;
    String email;
}
