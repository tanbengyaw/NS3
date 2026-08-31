package my.gov.perkeso.assist.registration.exception;

import my.gov.perkeso.assist.core.infrastructure.exception.AbstractPlatformDomainRuleException;

public class EmployerCodeDuplicateException extends AbstractPlatformDomainRuleException {

    public EmployerCodeDuplicateException(final String employerCode) {
        super("Employer code already exists: " + employerCode);
    }
}
