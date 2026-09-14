package my.gov.perkeso.assist.audit.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuditFindingsData {

    Long id;
    String summaryDetail;
    Integer supervisorStatus;
    String supervisorRemark;
}
