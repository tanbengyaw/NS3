package my.gov.perkeso.assist.registration.data;

import lombok.Builder;
import lombok.Value;

/**
 * One row of the "search existing taxpayers for update" results (GET
 * /v1/reference/tax-payer-updates).
 */
@Value
@Builder
public class TaxPayerUpdateSearchResultData {

    Long employerId;
    String employerName;
    String registrationNo;
    String smkRegNo;
    Long sectionId;
}
