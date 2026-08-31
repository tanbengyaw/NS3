package my.gov.perkeso.assist.registration.data;

import lombok.Builder;
import lombok.Getter;
import my.gov.perkeso.assist.registration.constant.RegistrationSpecialCaseType;

@Getter
@Builder
public class RegistrationSpecialCase {

    private final RegistrationSpecialCaseType type;
    private final String message;
}
