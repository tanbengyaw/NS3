package my.gov.perkeso.assist.identity.service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.security.PlatformUser;
import my.gov.perkeso.assist.core.security.PlatformUserContext;
import my.gov.perkeso.assist.core.security.PlatformUserRole;
import my.gov.perkeso.assist.identity.data.CreateStaffUserRequest;
import my.gov.perkeso.assist.identity.data.StaffUserData;
import my.gov.perkeso.assist.identity.data.UpdateStaffUserRequest;
import my.gov.perkeso.assist.identity.domain.StaffUser;
import my.gov.perkeso.assist.identity.domain.StaffUserRepository;
import my.gov.perkeso.assist.identity.domain.StaffUserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffUserWritePlatformService {

    private final StaffUserRepository staffUserRepository;
    private final PlatformUserContext platformUserContext;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public StaffUserData createStaffUser(final CreateStaffUserRequest request) {
        assertAdmin();

        requireNonBlank(request.getUsername(), "username");
        requireNonBlank(request.getEmail(), "email");
        requireNonBlank(request.getPassword(), "password");
        StaffRoleValidator.validateRoles(request.getRoles());

        final String username = request.getUsername().trim();
        final String email = request.getEmail().trim();

        staffUserRepository.findByUsernameIgnoreCase(username).ifPresent(existing -> {
            throw new IllegalArgumentException("Staff username already exists: " + username);
        });
        staffUserRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            throw new IllegalArgumentException("Staff email already exists: " + email);
        });

        final LocalDateTime now = LocalDateTime.now();
        final StaffUser staffUser = new StaffUser();
        staffUser.setUsername(username);
        staffUser.setEmail(email);
        staffUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        staffUser.setBranchId(request.getBranchId());
        staffUser.setActive(request.getActive() == null || request.getActive());
        staffUser.setCreatedDate(now);
        staffUser.setUpdatedDate(now);
        replaceRoles(staffUser, request.getRoles());

        return StaffUserMapper.toData(staffUserRepository.save(staffUser));
    }

    @Transactional
    public StaffUserData updateStaffUser(final Long staffUserId, final UpdateStaffUserRequest request) {
        assertAdmin();

        final StaffUser staffUser = staffUserRepository.findById(staffUserId)
                .orElseThrow(() -> new IllegalArgumentException("Staff user not found: " + staffUserId));

        if (request.getEmail() != null) {
            final String email = request.getEmail().trim();
            requireNonBlank(email, "email");
            staffUserRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
                if (!existing.getId().equals(staffUserId)) {
                    throw new IllegalArgumentException("Staff email already exists: " + email);
                }
            });
            staffUser.setEmail(email);
        }

        if (request.getBranchId() != null) {
            staffUser.setBranchId(request.getBranchId());
        }

        if (request.getActive() != null) {
            staffUser.setActive(request.getActive());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            staffUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRoles() != null) {
            StaffRoleValidator.validateRoles(request.getRoles());
            replaceRoles(staffUser, request.getRoles());
        }

        staffUser.setUpdatedDate(LocalDateTime.now());
        return StaffUserMapper.toData(staffUserRepository.save(staffUser));
    }

    private static void replaceRoles(final StaffUser staffUser, final List<String> roles) {
        staffUser.getRoles().clear();
        final Set<String> uniqueRoles = new LinkedHashSet<>();
        for (final String role : roles) {
            uniqueRoles.add(StaffRoleValidator.normalizeRole(role));
        }
        for (final String roleCode : uniqueRoles) {
            final StaffUserRole staffUserRole = new StaffUserRole();
            staffUserRole.setStaffUser(staffUser);
            staffUserRole.setRoleCode(roleCode);
            staffUser.getRoles().add(staffUserRole);
        }
    }

    private void assertAdmin() {
        final PlatformUser currentUser = platformUserContext.getCurrentUser();
        if (!currentUser.roles().contains(PlatformUserRole.ADMIN)) {
            throw new IllegalStateException("Only ADMIN users can manage staff accounts");
        }
    }

    private static void requireNonBlank(final String value, final String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
