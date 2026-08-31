package my.gov.perkeso.assist.registration.service;

import java.util.List;
import my.gov.perkeso.assist.core.commands.api.JsonCommand;
import my.gov.perkeso.assist.core.security.PlatformUser;
import my.gov.perkeso.assist.registration.constant.RegistrationSpecialCaseType;
import my.gov.perkeso.assist.registration.data.RegistrationSpecialCase;
import my.gov.perkeso.assist.registration.domain.AppStatus;
import org.springframework.stereotype.Component;

/**
 * Mirrors ASSIST {@code NewRegCounter#checkIncomplete} and special-case submit routing.
 */
@Component
public class RegistrationCaseSubmitRouter {

    public AppStatus resolveSubmitStatus(final PlatformUser user, final JsonCommand command,
            final List<RegistrationSpecialCase> specialCases) {
        if (user.isEmployer()) {
            if (!specialCases.isEmpty()) {
                return AppStatus.IN_PROGRESS;
            }
            return AppStatus.SUBMITTED;
        }
        if (user.isRo()) {
            return AppStatus.APPROVED;
        }
        if (Boolean.TRUE.equals(command.booleanValueOfParameterNamed("incomplete"))) {
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

    public boolean shouldRejectRoDuplicateBrn(final PlatformUser user, final List<RegistrationSpecialCase> specialCases) {
        return user.isRo() && specialCases.stream()
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
