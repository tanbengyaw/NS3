package my.gov.perkeso.assist.registration.data;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaxRegistrantRegistrationInfoData {

    private final TaxRegistrantCompanyInfoData companyInfo;
    private final List<TaxRegistrantRegistrationTypeRowData> registrationTypes;
}
