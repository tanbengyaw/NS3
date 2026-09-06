package my.gov.perkeso.assist.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import my.gov.perkeso.assist.core.security.PlatformUser;
import my.gov.perkeso.assist.core.security.PlatformUserContext;
import my.gov.perkeso.assist.core.security.PlatformUserRole;
import my.gov.perkeso.assist.identity.data.CreateStaffUserRequest;
import my.gov.perkeso.assist.identity.domain.StaffUser;
import my.gov.perkeso.assist.identity.domain.StaffUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class StaffUserWritePlatformServiceTest {

    @Mock
    private StaffUserRepository staffUserRepository;

    @Mock
    private PlatformUserContext platformUserContext;

    @Mock
    private PasswordEncoder passwordEncoder;

    private StaffUserWritePlatformService service;

    @BeforeEach
    void setUp() {
        service = new StaffUserWritePlatformService(staffUserRepository, platformUserContext, passwordEncoder);
        when(platformUserContext.getCurrentUser()).thenReturn(adminUser());
    }

    @Test
    void createStaffUserPersistsEncodedPasswordAndRoles() {
        when(staffUserRepository.findByUsernameIgnoreCase("new_ro")).thenReturn(Optional.empty());
        when(staffUserRepository.findByEmailIgnoreCase("new.ro@perkeso.example")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("{bcrypt}encoded");
        when(staffUserRepository.save(any(StaffUser.class))).thenAnswer(invocation -> {
            final StaffUser saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        final var result = service.createStaffUser(CreateStaffUserRequest.builder()
                .username("new_ro")
                .email("new.ro@perkeso.example")
                .password("secret")
                .branchId(2L)
                .roles(List.of("RO"))
                .build());

        assertThat(result.getUsername()).isEqualTo("new_ro");
        assertThat(result.getRoles()).containsExactly("RO");

        final ArgumentCaptor<StaffUser> captor = ArgumentCaptor.forClass(StaffUser.class);
        verify(staffUserRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("{bcrypt}encoded");
        assertThat(captor.getValue().getBranchId()).isEqualTo(2L);
    }

    @Test
    void createStaffUserRequiresAdmin() {
        when(platformUserContext.getCurrentUser()).thenReturn(officerUser());

        assertThatThrownBy(() -> service.createStaffUser(CreateStaffUserRequest.builder()
                .username("new_ro")
                .email("new.ro@perkeso.example")
                .password("secret")
                .roles(List.of("RO"))
                .build())).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN");
    }

    private static PlatformUser adminUser() {
        return new PlatformUser("admin", "officer@perkeso.example", EnumSet.of(PlatformUserRole.ADMIN), 2L);
    }

    private static PlatformUser officerUser() {
        return new PlatformUser("officer_pj", "officer.pj@perkeso.example", EnumSet.of(PlatformUserRole.OFFICER), 3L);
    }
}
