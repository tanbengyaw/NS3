package my.gov.perkeso.assist.registration.constant;

/**
 * Mirrors ASSIST {@code DataSourceEnum}.
 */
public enum DataSource {
    OTC(1L),
    PORTAL(2L),
    EMAILORPOSTAL(3L);

    private final long assistId;

    DataSource(final long assistId) {
        this.assistId = assistId;
    }

    public long getAssistId() {
        return assistId;
    }

    public static DataSource fromAssistId(final long assistId) {
        for (final DataSource source : values()) {
            if (source.assistId == assistId) {
                return source;
            }
        }
        throw new IllegalArgumentException("Data source not found for id: " + assistId);
    }
}
