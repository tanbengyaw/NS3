package my.gov.perkeso.assist.registration.data;

import lombok.Builder;
import lombok.Value;

/** One row of the "search active taxpayers for discontinue-tax" results. */
@Value
@Builder
public class DiscontinueTaxSearchResultData {

    Long employerId;
    String employerName;
    String registrationNo;
    String smkRegNo;
    Long sstInfoId;
    Long taxTypeId;
    String taxTypeLabel;
}
