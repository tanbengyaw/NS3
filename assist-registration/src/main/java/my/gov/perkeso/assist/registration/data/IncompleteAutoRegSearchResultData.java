package my.gov.perkeso.assist.registration.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IncompleteAutoRegSearchResultData {

    Long sstInfoId;
    Long employerId;
    String employerName;
    String registrationNo;
    String taxType;
    Long sectionId;
    String cusAudRefNo;
    LocalDate businessComDate;
    LocalDateTime createdDate;
    Long openCaseId;
    String openCaseRefNo;
}
