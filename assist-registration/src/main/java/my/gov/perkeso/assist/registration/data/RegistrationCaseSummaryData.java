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
    private final String employerCode;
    private final String salesTaxSmkRegNo;
    private final String queryRemark;
    private final String appStatusReason;
    private final LocalDateTime submissionDate;
    private final LocalDateTime createdDate;
}
