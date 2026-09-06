package my.gov.perkeso.assist.registration.service;

public record BaseUserEmployerEnrollmentCommand(
        Long portalUserId,
        int applicationType,
        String employerCode,
        String employerName,
        Long registrationTypeId,
        String registrationNo,
        String addressLine1,
        String addressLine2,
        String addressLine3,
        Long stateId,
        Long cityId,
        String postCode,
        String fullName,
        Long identificationTypeId,
        String identificationNo,
        String phoneCallingCode,
        String phoneNumber,
        String email,
        String securityPhrase,
        Long registrationEmployerId,
        String draftToken) {
}
