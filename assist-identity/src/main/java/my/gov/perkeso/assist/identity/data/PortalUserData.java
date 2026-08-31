package my.gov.perkeso.assist.identity.data;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PortalUserData {

    private final Long id;
    private final String username;
    private final String email;
    private final String applicationType;
    private final Long employerId;
    private final String employerCode;
    private final LocalDateTime enrolledDate;
    private final LocalDateTime linkedDate;
}
