package my.gov.perkeso.assist.registration.data;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TempEmployeeData {

    private final Long id;
    private final Long caseId;
    private final String employeeName;
    private final String identificationNo;
    private final LocalDate employmentStartDate;
    private final Long nationalityId;
}
