package my.gov.perkeso.assist.registration.data;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

/** Draft data for a Discontinue Tax case's own form (GET/PUT .../discontinue-info). */
@Value
@Builder
public class DiscontinueTaxInfoData {

    Long caseId;
    Long sstInfoId;
    Long taxTypeId;
    String taxTypeLabel;
    Long currentSstStatusId;
    String currentSstStatusLabel;
    Long newSstStatusId;
    LocalDate cessationTaxEffectiveFrom;
}
