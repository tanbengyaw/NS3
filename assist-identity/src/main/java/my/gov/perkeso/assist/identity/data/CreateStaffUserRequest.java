package my.gov.perkeso.assist.identity.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateStaffUserRequest {

    private final String username;
    private final String email;
    private final String password;
    private final Long branchId;
    private final List<String> roles;
    private final Boolean active;
}
