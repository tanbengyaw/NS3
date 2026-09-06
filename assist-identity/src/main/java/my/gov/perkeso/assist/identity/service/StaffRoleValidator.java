package my.gov.perkeso.assist.identity.service;

import java.util.Set;
import my.gov.perkeso.assist.core.security.PlatformUserRole;

final class StaffRoleValidator {

    private static final Set<String> ALLOWED_ROLES = Set.of(
            PlatformUserRole.ADMIN.name(),
            PlatformUserRole.OFFICER.name(),
            PlatformUserRole.RO.name(),
            PlatformUserRole.UO.name(),
            PlatformUserRole.PKR_BO.name(),
            PlatformUserRole.EMPLOYER.name());

    private StaffRoleValidator() {
    }

    static void validateRoles(final Iterable<String> roles) {
        if (roles == null) {
            throw new IllegalArgumentException("roles is required");
        }
        boolean hasRole = false;
        for (final String role : roles) {
            hasRole = true;
            if (role == null || role.isBlank()) {
                throw new IllegalArgumentException("roles must not contain blank values");
            }
            final String normalized = role.trim().toUpperCase();
            if (!ALLOWED_ROLES.contains(normalized)) {
                throw new IllegalArgumentException("Unsupported role: " + role);
            }
        }
        if (!hasRole) {
            throw new IllegalArgumentException("At least one role is required");
        }
    }

    static String normalizeRole(final String role) {
        return role.trim().toUpperCase();
    }
}
