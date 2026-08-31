package my.gov.perkeso.assist.registration.exception;

import my.gov.perkeso.assist.core.infrastructure.exception.AbstractPlatformDomainRuleException;

public class RegistrationCaseInvalidStatusException extends AbstractPlatformDomainRuleException {

    public RegistrationCaseInvalidStatusException(final String caseRefNo, final String currentStatus, final String expected) {
        super("Case " + caseRefNo + " is " + currentStatus + " but expected " + expected);
    }
}
