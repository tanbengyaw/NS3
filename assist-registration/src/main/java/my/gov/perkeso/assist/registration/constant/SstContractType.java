package my.gov.perkeso.assist.registration.constant;

/**
 * Mirrors ASSIST {@code SstContractTypeEnum}.
 */
public enum SstContractType {
    MAIN_CONTRACT(1L),
    SUB_CONTRACT(2L);

    private final long assistId;

    SstContractType(final long assistId) {
        this.assistId = assistId;
    }

    public long getAssistId() {
        return assistId;
    }
}
