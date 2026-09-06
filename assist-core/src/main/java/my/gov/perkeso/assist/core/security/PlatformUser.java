package my.gov.perkeso.assist.core.security;

import java.util.Set;

public record PlatformUser(String username, String email, Set<PlatformUserRole> roles, Long officeId) {

    public boolean isEmployer() {
        return roles.contains(PlatformUserRole.EMPLOYER);
    }

    public boolean isRo() {
        return roles.contains(PlatformUserRole.RO);
    }

    public boolean isUo() {
        return roles.contains(PlatformUserRole.UO);
    }

    public boolean isPkrBo() {
        return roles.contains(PlatformUserRole.PKR_BO);
    }

    public boolean isOfficer() {
        return roles.contains(PlatformUserRole.OFFICER) || roles.contains(PlatformUserRole.ADMIN)
                || roles.contains(PlatformUserRole.RO) || roles.contains(PlatformUserRole.UO)
                || roles.contains(PlatformUserRole.PKR_BO);
    }
}
