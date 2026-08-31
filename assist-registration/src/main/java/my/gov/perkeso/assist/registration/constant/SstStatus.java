package my.gov.perkeso.assist.registration.constant;

/**
 * Mirrors ASSIST {@code SstStatusTypeEnum} — Active / Cancel.
 */
public enum SstStatus {
    ACTIVE(1L),
    CANCEL(2L);

    private final long assistId;

    SstStatus(final long assistId) {
        this.assistId = assistId;
    }

    public long getAssistId() {
        return assistId;
    }
}
