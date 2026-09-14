package my.gov.perkeso.assist.audit.constant;

/**
 * Legacy {@code BASE.REF_TASK_STATUS} 1100–1108 (May 2026 Audit redesign).
 */
public final class AuditTaskStatus {

    public static final long PENDING_CASE_CREATION = 1100L;
    public static final long FIELDWORK = 1101L;

    private AuditTaskStatus() {
    }

    public static String label(final Long id) {
        if (id == null) {
            return null;
        }
        return switch (id.intValue()) {
            case 1100 -> "Pending Audit Case Creation";
            case 1101 -> "Fieldwork";
            case 1102 -> "Pending Audit Approval";
            case 1103 -> "Audit Approved - Pending Taxpayer";
            case 1104 -> "Under Review";
            case 1105 -> "Closed For Monitor";
            case 1106 -> "Closed For Appeal";
            case 1107 -> "Closed Case";
            case 1108 -> "Open";
            default -> "Status " + id;
        };
    }
}
