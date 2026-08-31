package my.gov.perkeso.assist.registration.employercode;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmployerCodeContext {

    private final Long branchId;
    private final String postCode;
    private final String areaCode;
}
