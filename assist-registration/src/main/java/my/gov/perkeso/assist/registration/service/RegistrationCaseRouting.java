package my.gov.perkeso.assist.registration.service;

/**
 * Who currently owns a registration case. Routing is by role + processing branch
 * (not a single assigned user), except {@code IN_QUERY} which returns to the
 * person who submitted the case.
 */
public final class RegistrationCaseRouting {

    public record Target(String role, String username, String label) {}

    private RegistrationCaseRouting() {}

    /**
     * Update Tax Payer (1200–1204) and Discontinue Tax (1103) go to the UO queue.
     * Other sections stay on the officer queue.
     */
    public static boolean isUoWorkflowSection(final Long sectionId) {
        if (sectionId == null) {
            return false;
        }
        return sectionId == 1103L || (sectionId >= 1200L && sectionId <= 1204L);
    }

    public static Target resolve(final String appStatus, final Long sectionId, final String branchName,
            final String submittedByUsername, final String createdByUsername) {
        if (appStatus == null) {
            return null;
        }
        return switch (appStatus) {
            case "IN_QUERY" -> submitterTarget(sectionId, submittedByUsername, createdByUsername);
            case "SUBMITTED", "IN_PROGRESS" -> queueTarget(sectionId, branchName);
            default -> null;
        };
    }

    private static Target submitterTarget(final Long sectionId, final String submittedByUsername,
            final String createdByUsername) {
        final String username = firstNonBlank(submittedByUsername, createdByUsername);
        final boolean staffCase = isUoWorkflowSection(sectionId);
        final String role = staffCase ? "SUBMITTER" : "APPLICANT";
        final String who = staffCase ? "Submitting officer" : "Applicant";
        final String label = username == null ? who : who + " (" + username + ")";
        return new Target(role, username, label);
    }

    private static Target queueTarget(final Long sectionId, final String branchName) {
        final boolean uo = isUoWorkflowSection(sectionId);
        final String role = uo ? "UO" : "OFFICER";
        final String queue = uo ? "UO" : "Officer";
        final String label = branchName == null || branchName.isBlank() ? queue : queue + " — " + branchName;
        return new Target(role, null, label);
    }

    private static String firstNonBlank(final String first, final String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }
}
