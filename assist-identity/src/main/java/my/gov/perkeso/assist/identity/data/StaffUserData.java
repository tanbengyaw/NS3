package my.gov.perkeso.assist.identity.data;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffUserData {

    private final Long id;
    private final String username;
    private final String email;
    private final Long branchId;
    private final List<String> roles;
    private final boolean active;
    private final LocalDateTime createdDate;
    private final LocalDateTime updatedDate;
}
