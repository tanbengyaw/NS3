package my.gov.perkeso.assist.identity.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException;
import my.gov.perkeso.assist.core.security.PlatformUser;
import my.gov.perkeso.assist.core.security.PlatformUserContext;
import my.gov.perkeso.assist.core.security.PlatformUserRole;
import my.gov.perkeso.assist.identity.data.StaffUserData;
import my.gov.perkeso.assist.identity.domain.StaffUser;
import my.gov.perkeso.assist.identity.domain.StaffUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffUserReadPlatformService {

    private final StaffUserRepository staffUserRepository;
    private final PlatformUserContext platformUserContext;

    public List<StaffUserData> retrieveAll() {
        assertAdmin();
        return staffUserRepository.findAllByOrderByUsernameAsc().stream()
                .map(StaffUserMapper::toData)
                .toList();
    }

    public StaffUserData retrieveById(final Long staffUserId) {
        assertAdmin();
        return StaffUserMapper.toData(findStaffUser(staffUserId));
    }

    public StaffUserData retrieveByUsername(final String username) {
        return staffUserRepository.findByUsernameIgnoreCase(username)
                .map(StaffUserMapper::toData)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found: " + username));
    }

    /**
     * Own-profile lookup for the currently authenticated user (username/roles/branch) — no ADMIN
     * check, since every logged-in user is entitled to see their own profile. This backs the
     * frontend's post-login role fetch so the UI never needs a hardcoded username-to-role map.
     */
    public StaffUserData retrieveCurrent() {
        return retrieveByUsername(platformUserContext.getCurrentUser().username());
    }

    public StaffUser findActiveStaffUserByUsername(final String username) {
        final StaffUser staffUser = staffUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found: " + username));
        if (!staffUser.isActive()) {
            throw new ResourceNotFoundException("Staff user is inactive: " + username);
        }
        return staffUser;
    }

    private StaffUser findStaffUser(final Long staffUserId) {
        return staffUserRepository.findById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found: " + staffUserId));
    }

    private void assertAdmin() {
        final PlatformUser currentUser = platformUserContext.getCurrentUser();
        if (!currentUser.roles().contains(PlatformUserRole.ADMIN)) {
            throw new IllegalStateException("Only ADMIN users can manage staff accounts");
        }
    }
}
