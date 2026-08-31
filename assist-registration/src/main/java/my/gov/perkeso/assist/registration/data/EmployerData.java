package my.gov.perkeso.assist.registration.data;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmployerData {

    private final Long id;
    private final String employerCode;
    private final String employerName;
    private final String registrationNo;
    private final Long serviceTypeId;
    private final Long pksBranchId;
    private final boolean branch;
    private final Long msicId;
    private final boolean contributionActive;
    private final String operationalStatus;
    private final LocalDateTime createdDate;
}
