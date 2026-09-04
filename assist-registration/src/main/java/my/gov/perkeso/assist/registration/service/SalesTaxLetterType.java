package my.gov.perkeso.assist.registration.service;

import my.gov.perkeso.assist.registration.domain.AppStatus;

public enum SalesTaxLetterType {
    ACKNOWLEDGEMENT("acknowledgement", "sales-tax-acknowledgement", AppStatus.APPROVED),
    INQUIRY("inquiry", "sales-tax-inquiry", AppStatus.IN_QUERY),
    REJECTION("rejection", "sales-tax-rejection", AppStatus.REJECTED);

    private final String queryValue;
    private final String filePrefix;
    private final AppStatus requiredStatus;

    SalesTaxLetterType(final String queryValue, final String filePrefix, final AppStatus requiredStatus) {
        this.queryValue = queryValue;
        this.filePrefix = filePrefix;
        this.requiredStatus = requiredStatus;
    }

    public String queryValue() {
        return queryValue;
    }

    public String filePrefix() {
        return filePrefix;
    }

    public AppStatus requiredStatus() {
        return requiredStatus;
    }

    public static SalesTaxLetterType fromQueryParam(final String letterType) {
        if (letterType == null || letterType.isBlank()) {
            return ACKNOWLEDGEMENT;
        }
        final String normalized = letterType.trim().toLowerCase();
        for (final SalesTaxLetterType type : values()) {
            if (type.queryValue.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "Unsupported letter type: " + letterType + " (use acknowledgement, inquiry, or rejection)");
    }
}
