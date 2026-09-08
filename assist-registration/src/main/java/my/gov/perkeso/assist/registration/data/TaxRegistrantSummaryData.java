package my.gov.perkeso.assist.registration.data;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaxRegistrantSummaryData {

    private final String taxpayerName;
    private final String sstRegistrationNo;
}
