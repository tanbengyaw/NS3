package my.gov.perkeso.assist.registration.data;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaxPayerDirectorData {

    private final String name;
    private final Long identificationTypeId;
    private final String identificationNo;
    private final String email;
    private final String designation;
}
