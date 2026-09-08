package my.gov.perkeso.assist.registration.data;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaxRegistrantRegistrationTypeRowData {

    private final String taxType;
    private final String sstRegistrationNo;
    private final LocalDateTime registeredDate;
    private final String status;
}
