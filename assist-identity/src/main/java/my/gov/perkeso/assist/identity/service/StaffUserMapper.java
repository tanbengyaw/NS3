package my.gov.perkeso.assist.identity.service;

import java.util.Comparator;
import java.util.List;
import my.gov.perkeso.assist.identity.data.StaffUserData;
import my.gov.perkeso.assist.identity.domain.StaffUser;
import my.gov.perkeso.assist.identity.domain.StaffUserRole;

public final class StaffUserMapper {

    private StaffUserMapper() {
    }

    public static StaffUserData toData(final StaffUser staffUser) {
        return StaffUserData.builder()
                .id(staffUser.getId())
                .username(staffUser.getUsername())
                .email(staffUser.getEmail())
                .branchId(staffUser.getBranchId())
                .roles(roleCodes(staffUser))
                .active(staffUser.isActive())
                .createdDate(staffUser.getCreatedDate())
                .updatedDate(staffUser.getUpdatedDate())
                .build();
    }

    public static List<String> roleCodes(final StaffUser staffUser) {
        return staffUser.getRoles().stream()
                .map(StaffUserRole::getRoleCode)
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
