package my.gov.perkeso.assist.audit.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuditFieldWorkData {

    Long id;
    Long refVisitTypeId;
    String other;
    String observation;
    Long refSiteVisitOutcomeId;
    String officerRemark;
}
