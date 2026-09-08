package my.gov.perkeso.assist.infrastructure.security;

import java.util.List;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException;
import my.gov.perkeso.assist.core.security.PlatformUserRole;
import my.gov.perkeso.assist.identity.constant.PortalEnrollmentStatus;
import my.gov.perkeso.assist.identity.domain.StaffUser;
import my.gov.perkeso.assist.identity.domain.StaffUserRepository;
import my.gov.perkeso.assist.identity.service.StaffUserMapper;
import my.gov.perkeso.assist.registration.domain.PortalUser;
import my.gov.perkeso.assist.registration.domain.PortalUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DbUserDetailsService implements UserDetailsService {

    private final StaffUserRepository staffUserRepository;
    private final PortalUserRepository portalUserRepository;

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        try {
            return loadStaffUser(username);
        } catch (UsernameNotFoundException staffNotFound) {
            try {
                return loadPortalUser(username);
            } catch (UsernameNotFoundException portalNotFound) {
                throw new UsernameNotFoundException("User not found: " + username, portalNotFound);
            }
        }
    }

    private AssistUserDetails loadStaffUser(final String username) {
        try {
            final StaffUser staffUser = staffUserRepository.findByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Staff user not found: " + username));
            if (!staffUser.isActive()) {
                throw new ResourceNotFoundException("Staff user is inactive: " + username);
            }
            return new AssistUserDetails(staffUser.getUsername(), staffUser.getPasswordHash(),
                    StaffUserMapper.roleCodes(staffUser), staffUser.getBranchId(), staffUser.getEmail());
        } catch (RuntimeException ex) {
            throw new UsernameNotFoundException("Staff user not found: " + username, ex);
        }
    }

    private AssistUserDetails loadPortalUser(final String username) {
        try {
            final PortalUser portalUser = portalUserRepository.findByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Portal user not found: " + username));
            if (!PortalEnrollmentStatus.APPROVED.name().equals(portalUser.getEnrollmentStatus())
                    || !portalUser.isLoginEnabled()) {
                throw new ResourceNotFoundException("Portal login is not active for user: " + username);
            }
            return new AssistUserDetails(portalUser.getUsername(), portalUser.getPasswordHash(),
                    List.of(PlatformUserRole.EMPLOYER.name()), null, portalUser.getEmail());
        } catch (RuntimeException ex) {
            throw new UsernameNotFoundException("Portal user not found: " + username, ex);
        }
    }
}
