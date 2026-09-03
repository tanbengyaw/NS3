package my.gov.perkeso.assist.registration.data;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegistrationCaseSummaryData {

    private final Long id;
    private final String caseRefNo;
    private final String appStatus;
    private final Long sectionId;
    private final String sectionCode;
    private final String employerName;
    private final String registrationNo;
    private final LocalDateTime submissionDate;
    private final LocalDateTime createdDate;
}
