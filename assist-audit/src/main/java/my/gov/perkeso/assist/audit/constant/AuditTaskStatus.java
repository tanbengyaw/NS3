package my.gov.perkeso.assist.audit.constant;

/**
 * Legacy {@code BASE.REF_TASK_STATUS} 1100–1108 (May 2026 Audit redesign).
 */
public final class AuditTaskStatus {

    public static final long PENDING_CASE_CREATION = 1100L;
    public static final long FIELDWORK = 1101L;
    public static final long PENDING_AUDIT_APPROVAL = 1102L;
    public static final long AUDIT_APPROVED_PENDING_TAXPAYER = 1103L;
    public static final long UNDER_REVIEW = 1104L;
    public static final long CLOSED_FOR_APPEAL = 1106L;
    public static final long CLOSED_CASE = 1107L;
    public static final long FIELD_CASE_TYPE = 2L;

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
