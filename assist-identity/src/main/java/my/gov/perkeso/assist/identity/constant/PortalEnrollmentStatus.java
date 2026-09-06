package my.gov.perkeso.assist.identity.constant;

public enum PortalEnrollmentStatus {

    SUBMITTED,
    IN_QUERY,
    APPROVED,
    REJECTED;

    public static PortalEnrollmentStatus fromValue(final String value) {
        if (value == null || value.isBlank()) {
            return SUBMITTED;
        }
        return PortalEnrollmentStatus.valueOf(value.trim().toUpperCase());
    }
}
