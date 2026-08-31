package my.gov.perkeso.assist.identity.constant;

public enum PortalApplicationType {
    NEW_EMPLOYER,
    EXISTING_EMPLOYER;

    public static PortalApplicationType fromValue(final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("applicationType is required");
        }
        return PortalApplicationType.valueOf(value.trim().toUpperCase());
    }
}
