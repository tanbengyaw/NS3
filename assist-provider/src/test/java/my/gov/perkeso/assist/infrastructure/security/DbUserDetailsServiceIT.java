package my.gov.perkeso.assist.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DbUserDetailsServiceIT {

    @Autowired
    private DbUserDetailsService userDetailsService;

    @Test
    void loadsSeededAdminUser() {
        final var user = userDetailsService.loadUserByUsername("admin");
        assertThat(user.getUsername()).isEqualTo("admin");
        assertThat(user.getAuthorities()).extracting("authority")
                .contains("ROLE_ADMIN", "ROLE_OFFICER");
    }
}
