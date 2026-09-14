package my.gov.perkeso.assist.audit.data;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuditPlanningData {

    Long id;
    Long refProposedCaseTypeId;
    LocalDate timelineFrom;
    LocalDate timelineTo;
    String activitiesDetails;
    String exclusions;
    String limitationDisclosure;
}
