package my.gov.perkeso.assist.infrastructure.security;

import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.security.PlatformUser;
import my.gov.perkeso.assist.core.security.PlatformUserContext;
import my.gov.perkeso.assist.core.security.PlatformUserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringPlatformUserContext implements PlatformUserContext {

    private final AssistUserDetailsLookup assistUserDetailsLookup;

    @Override
    public PlatformUser getCurrentUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user in security context");
        }

        return assistUserDetailsLookup.findByUsername(authentication.getName())
                .map(assistUserDetails -> new PlatformUser(assistUserDetails.getUsername(),
                        assistUserDetails.getEmail(), mapRoles(assistUserDetails.getAuthorities()),
                        assistUserDetails.getOfficeId()))
                .orElseGet(() -> new PlatformUser(authentication.getName(), null,
                        mapRoles(authentication.getAuthorities()), null));
    }

    private static Set<PlatformUserRole> mapRoles(final Iterable<? extends GrantedAuthority> authorities) {
        final Set<PlatformUserRole> roles = EnumSet.noneOf(PlatformUserRole.class);
        for (final GrantedAuthority authority : authorities) {
            switch (authority.getAuthority()) {
                case "ROLE_EMPLOYER" -> roles.add(PlatformUserRole.EMPLOYER);
                case "ROLE_OFFICER" -> roles.add(PlatformUserRole.OFFICER);
                case "ROLE_RO" -> roles.add(PlatformUserRole.RO);
                case "ROLE_UO" -> roles.add(PlatformUserRole.UO);
                case "ROLE_PKR_BO" -> roles.add(PlatformUserRole.PKR_BO);
                case "ROLE_ADMIN" -> roles.add(PlatformUserRole.ADMIN);
                default -> {
                    // ignore unknown roles
                }
            }
        }
        return roles;
    }
}
