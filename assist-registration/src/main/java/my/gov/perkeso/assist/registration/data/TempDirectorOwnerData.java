package my.gov.perkeso.assist.registration.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TempDirectorOwnerData {

    Long id;
    Long caseId;
    String name;
    Long identificationTypeId;
    String identificationNo;
    String email;
    String designation;
}
