package my.gov.perkeso.assist.audit.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuditTaxpayerResponseData {

    Long id;
    String responseId;
    LocalDate responseDate;
    Integer responseChannel;
    Integer responseType;
    Long employerId;
    String taxpayerComments;
    Integer officersStatus;
    Integer officersFinalOutcome;
    String officersRemarks;
    boolean activated;
    BigDecimal revisionAmount;
    String revisionReason;
    String cancellationId;
}
