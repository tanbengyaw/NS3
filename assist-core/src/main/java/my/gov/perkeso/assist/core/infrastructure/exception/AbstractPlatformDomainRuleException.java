package my.gov.perkeso.assist.core.infrastructure.exception;

public abstract class AbstractPlatformDomainRuleException extends RuntimeException {

    protected AbstractPlatformDomainRuleException(final String message) {
        super(message);
    }
}
