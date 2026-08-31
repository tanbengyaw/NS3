package my.gov.perkeso.assist.registration.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TempSstTariffCodeData {

    Long id;
    Long tariffCodeSalesTypeId;
    String tariffCode;
    String tariffDescription;
    Long contractTypeId;
    String finishedGoods;
}
