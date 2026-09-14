package my.gov.perkeso.assist.audit.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuditCaseListingData {

    Long id;
    String caseRefNo;
    Long taskStatusId;
    String taskStatusLabel;
    boolean submitted;
    String taxPayerName;
    String businessRegNo;
    Long employerId;
}
