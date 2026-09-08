package my.gov.perkeso.assist.registration.data;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SstNotificationData {

    private final Long id;
    private final String details;
    private final LocalDateTime createdDate;
}
