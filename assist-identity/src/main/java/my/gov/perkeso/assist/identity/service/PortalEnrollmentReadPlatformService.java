package my.gov.perkeso.assist.identity.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException;
import my.gov.perkeso.assist.core.security.PlatformUserContext;
import my.gov.perkeso.assist.identity.data.PortalUserData;
import my.gov.perkeso.assist.registration.domain.PortalUser;
import my.gov.perkeso.assist.registration.domain.PortalUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortalEnrollmentReadPlatformService {

    private final PortalUserRepository portalUserRepository;
    private final PlatformUserContext platformUserContext;

    @Transactional(readOnly = true)
    public PortalUserData retrieveCurrentPortalUser() {
        final String username = platformUserContext.getCurrentUser().username();
        final PortalUser portalUser = portalUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Portal user not found: " + username));
        return toData(portalUser);
    }

    @Transactional(readOnly = true)
    public PortalUserData retrieveByUsername(final String username) {
        final PortalUser portalUser = portalUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Portal user not found: " + username));
        return toData(portalUser);
    }

    private static PortalUserData toData(final PortalUser portalUser) {
        return PortalUserMapper.toData(portalUser);
    }
}
