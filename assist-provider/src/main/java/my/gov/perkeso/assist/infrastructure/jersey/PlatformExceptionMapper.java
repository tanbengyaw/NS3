package my.gov.perkeso.assist.infrastructure.jersey;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import my.gov.perkeso.assist.core.infrastructure.data.ApiGlobalErrorResponse;
import my.gov.perkeso.assist.core.infrastructure.exception.AbstractPlatformDomainRuleException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@Provider
public class PlatformExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(final Exception exception) {
        if (exception instanceof AbstractPlatformDomainRuleException domainException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiGlobalErrorResponse.badRequest("validation.msg.domain.rule.violation",
                            domainException.getMessage()))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        if (exception instanceof IllegalArgumentException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiGlobalErrorResponse.badRequest("validation.msg.invalid.argument", exception.getMessage()))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        if (exception instanceof SecurityException) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiGlobalErrorResponse.forbidden(exception.getMessage()))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        if (exception instanceof DataIntegrityViolationException integrityException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiGlobalErrorResponse.badRequest("validation.msg.duplicate",
                            resolveIntegrityMessage(integrityException)))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ApiGlobalErrorResponse.internalError(exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static String resolveIntegrityMessage(final DataIntegrityViolationException exception) {
        final String message = exception.getMostSpecificCause().getMessage();
        if (message == null) {
            return "Duplicate or invalid value.";
        }
        final String lower = message.toLowerCase();
        if (lower.contains("staff_user") && lower.contains("username")) {
            return "Username is already in use.";
        }
        if (lower.contains("staff_user") && lower.contains("email")) {
            return "Email is already in use.";
        }
        if (lower.contains("primary key") || lower.contains("23505")) {
            return "Could not save staff user due to a database conflict. Restart the backend after upgrading.";
        }
        return "Duplicate or invalid value.";
    }
}
