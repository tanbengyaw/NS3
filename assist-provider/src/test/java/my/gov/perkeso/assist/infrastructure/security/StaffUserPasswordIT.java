package my.gov.perkeso.assist.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import my.gov.perkeso.assist.identity.domain.StaffUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class StaffUserPasswordIT {

    @Autowired
    private StaffUserRepository staffUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void seededAdminPasswordMatchesPassword() {
        final var admin = staffUserRepository.findByUsernameIgnoreCase("admin").orElseThrow();
        assertThat(admin.getPasswordHash()).startsWith("{bcrypt}$2a$");
        assertThat(passwordEncoder.matches("password", admin.getPasswordHash())).isTrue();
    }
}
