package my.gov.perkeso.assist.registration.service;

public enum SalesTaxAcknowledgementLetterFormat {
    PDF("application/pdf", ".pdf"),
    HTML("text/html; charset=UTF-8", ".html");

    private final String contentType;
    private final String fileExtension;

    SalesTaxAcknowledgementLetterFormat(final String contentType, final String fileExtension) {
        this.contentType = contentType;
        this.fileExtension = fileExtension;
    }

    public String contentType() {
        return contentType;
    }

    public String fileExtension() {
        return fileExtension;
    }

    public static SalesTaxAcknowledgementLetterFormat fromQueryParam(final String format) {
        if (format == null || format.isBlank()) {
            return PDF;
        }
        return switch (format.trim().toLowerCase()) {
            case "pdf" -> PDF;
            case "html" -> HTML;
            default -> throw new IllegalArgumentException("Unsupported letter format: " + format + " (use pdf or html)");
        };
    }
}
