package my.gov.perkeso.assist.registration.constant;

/**
 * Mirrors ASSIST {@code TaxTypeEnum}.
 */
public enum TaxType {
    SERVICE_TAX(1L),
    SALES_TAX(2L),
    TOURISM_TAX(3L),
    DIGITAL_TAX(4L),
    DPSP_TAX(5L);

    private final long assistId;

    TaxType(final long assistId) {
        this.assistId = assistId;
    }

    public long getAssistId() {
        return assistId;
    }
}
