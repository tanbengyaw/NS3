package my.gov.perkeso.assist.audit.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuditTaxpayerData {

    Long id;
    Long employerId;
    String taxPayerName;
    String businessRegNo;
    String addressLine1;
    String addressLine2;
    String addressLine3;
    Long refStateId;
    Long refCityId;
    String postcode;
    Long ns3BranchId;
}
