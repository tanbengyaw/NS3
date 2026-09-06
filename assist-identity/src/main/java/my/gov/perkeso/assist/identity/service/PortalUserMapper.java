package my.gov.perkeso.assist.identity.service;

import my.gov.perkeso.assist.identity.data.PortalUserData;
import my.gov.perkeso.assist.registration.domain.PortalUser;

final class PortalUserMapper {

    private PortalUserMapper() {
    }

    static PortalUserData toData(final PortalUser portalUser) {
        return PortalUserData.builder()
                .id(portalUser.getId())
                .username(portalUser.getUsername())
                .email(portalUser.getEmail())
                .applicationType(portalUser.getApplicationType())
                .employerId(portalUser.getEmployerId())
                .employerCode(portalUser.getEmployerCode())
                .employerName(portalUser.getEmployerName())
                .registrationTypeId(portalUser.getRegistrationTypeId())
                .registrationNo(portalUser.getRegistrationNo())
                .addressLine1(portalUser.getAddressLine1())
                .addressLine2(portalUser.getAddressLine2())
                .addressLine3(portalUser.getAddressLine3())
                .stateId(portalUser.getStateId())
                .cityId(portalUser.getCityId())
                .cityName(portalUser.getCityName())
                .postCode(portalUser.getPostCode())
                .fullName(portalUser.getFullName())
                .identificationTypeId(portalUser.getIdentificationTypeId())
                .identificationNo(portalUser.getIdentificationNo())
                .phoneCallingCode(portalUser.getPhoneCallingCode())
                .phoneNumber(portalUser.getPhoneNumber())
                .securityPhrase(portalUser.getSecurityPhrase())
                .enrollmentStatus(portalUser.getEnrollmentStatus())
                .queryRemark(portalUser.getQueryRemark())
                .enrolledDate(portalUser.getEnrolledDate())
                .linkedDate(portalUser.getLinkedDate())
                .build();
    }
}
