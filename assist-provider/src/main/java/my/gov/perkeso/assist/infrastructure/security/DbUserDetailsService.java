package my.gov.perkeso.assist.infrastructure.security;

import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException;
import my.gov.perkeso.assist.identity.domain.StaffUser;
import my.gov.perkeso.assist.identity.domain.StaffUserRepository;
import my.gov.perkeso.assist.identity.service.StaffUserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DbUserDetailsService implements UserDetailsService {

    private final StaffUserRepository staffUserRepository;

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        try {
            final StaffUser staffUser = staffUserRepository.findByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Staff user not found: " + username));
            if (!staffUser.isActive()) {
                throw new ResourceNotFoundException("Staff user is inactive: " + username);
            }
            return new AssistUserDetails(staffUser.getUsername(), staffUser.getPasswordHash(),
                    StaffUserMapper.roleCodes(staffUser), staffUser.getBranchId(), staffUser.getEmail());
        } catch (RuntimeException ex) {
            throw new UsernameNotFoundException("User not found: " + username, ex);
        }
    }
}
