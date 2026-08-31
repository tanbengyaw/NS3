package my.gov.perkeso.assist.infrastructure.jersey;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import my.gov.perkeso.assist.core.infrastructure.exception.AbstractPlatformDomainRuleException;
import my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

@Component
@Provider
public class PlatformExceptionMapper implements ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(final RuntimeException exception) {
        if (exception instanceof ResourceNotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiError("error.msg.resource.not.found", exception.getMessage())).build();
        }
        if (exception instanceof AbstractPlatformDomainRuleException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("validation.msg.domain.rule.violation", exception.getMessage())).build();
        }
        if (exception instanceof IllegalArgumentException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("validation.msg.invalid.argument", exception.getMessage())).build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ApiError("error.msg.internal", exception.getMessage())).build();
    }

    public record ApiError(String userMessageGlobalisationCode, String defaultUserMessage) {
    }
}
