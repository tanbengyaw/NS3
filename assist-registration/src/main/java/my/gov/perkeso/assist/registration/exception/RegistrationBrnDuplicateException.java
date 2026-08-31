package my.gov.perkeso.assist.registration.exception;

import my.gov.perkeso.assist.core.infrastructure.exception.AbstractPlatformDomainRuleException;

public class RegistrationBrnDuplicateException extends AbstractPlatformDomainRuleException {

    public RegistrationBrnDuplicateException(final String registrationNo) {
        super("Employer with registration number already exists: " + registrationNo);
    }
}
