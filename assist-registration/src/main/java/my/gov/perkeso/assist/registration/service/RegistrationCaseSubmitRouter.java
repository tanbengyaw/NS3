package my.gov.perkeso.assist.registration.service;

import java.util.List;
import my.gov.perkeso.assist.core.commands.api.JsonCommand;
import my.gov.perkeso.assist.core.security.PlatformUser;
import my.gov.perkeso.assist.registration.constant.RegistrationSectionRouting;
import my.gov.perkeso.assist.registration.constant.RegistrationSpecialCaseType;
import my.gov.perkeso.assist.registration.data.RegistrationSpecialCase;
import my.gov.perkeso.assist.registration.domain.AppStatus;
import org.springframework.stereotype.Component;

/**
 * Mirrors ASSIST {@code NewRegCounter#checkIncomplete}, {@code IncompleteAutoRegTaxCounter},
 * and update-tax UO routing ({@code RegWorkflowDriver}).
 */
@Component
public class RegistrationCaseSubmitRouter {

    public AppStatus resolveSubmitStatus(final PlatformUser user, final JsonCommand command,
            final List<RegistrationSpecialCase> specialCases, final Long sectionId) {
        final boolean incompleteFlag = isIncompleteSubmit(command);
        final boolean incompleteTaxSection = RegistrationSectionRouting.isIncompleteTaxSection(sectionId);
        final boolean uoWorkflowSection = RegistrationSectionRouting.isUoWorkflowSection(sectionId);

        if (user.isEmployer()) {
            if (!specialCases.isEmpty()) {
                return AppStatus.IN_PROGRESS;
            }
            return AppStatus.SUBMITTED;
        }

        // Incomplete auto-reg tax (1205–1209): non-UO staff auto-approve (legacy IncompleteAutoRegTaxCounter).
        if (incompleteTaxSection && !user.isUo()) {
            return AppStatus.APPROVED;
        }

        // RO auto-approves new registration only — not update/discontinue tax (1200–1204, 1103).
        if (user.isRo()) {
            if (uoWorkflowSection) {
                return AppStatus.SUBMITTED;
            }
            return AppStatus.APPROVED;
        }

        // PKR_BO auto-approves incomplete OTC/portal new-reg submissions.
        if (user.isPkrBo() && (incompleteFlag || incompleteTaxSection)) {
            return AppStatus.APPROVED;
        }

        // UO handles update/discontinue tax workflow queue.
        if (user.isUo()) {
            if (uoWorkflowSection) {
                return AppStatus.SUBMITTED;
            }
            if (incompleteFlag || !specialCases.isEmpty()) {
                return AppStatus.IN_PROGRESS;
            }
            return AppStatus.SUBMITTED;
        }

        // Generic counter staff (OFFICER, ADMIN).
        if (incompleteFlag) {
            return AppStatus.IN_PROGRESS;
        }
        if (!specialCases.isEmpty()) {
            return AppStatus.IN_PROGRESS;
        }
        return AppStatus.SUBMITTED;
    }

    public boolean isIncompleteSubmit(final JsonCommand command) {
        return Boolean.TRUE.equals(command.booleanValueOfParameterNamed("incomplete"));
    }

    public boolean shouldRejectRoDuplicateBrn(final PlatformUser user, final List<RegistrationSpecialCase> specialCases,
            final Long sectionId) {
        return user.isRo() && !RegistrationSectionRouting.isUoWorkflowSection(sectionId)
                && !RegistrationSectionRouting.isIncompleteTaxSection(sectionId)
                && specialCases.stream()
                        .anyMatch(item -> item.getType() == RegistrationSpecialCaseType.DUPLICATE_REGISTRATION_NUMBER);
    }

    public String duplicateBrnRejectReason(final List<RegistrationSpecialCase> specialCases) {
        return specialCases.stream()
                .filter(item -> item.getType() == RegistrationSpecialCaseType.DUPLICATE_REGISTRATION_NUMBER)
                .map(RegistrationSpecialCase::getMessage)
                .findFirst()
                .orElse("Business registration number already registered");
    }
}
