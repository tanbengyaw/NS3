package my.gov.perkeso.assist.registration.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TempSstServiceCategoryData {
    Long id;
    Long sstServiceTypeId;
    String serviceCode;
    String serviceDescription;
    String remark;
}
