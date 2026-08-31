package my.gov.perkeso.assist.registration.exception;

import my.gov.perkeso.assist.core.infrastructure.exception.AbstractPlatformDomainRuleException;

public class RegistrationCaseSubmitValidationException extends AbstractPlatformDomainRuleException {

    public RegistrationCaseSubmitValidationException(final String message) {
        super(message);
    }
}
