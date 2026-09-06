package my.gov.perkeso.assist.infrastructure.jersey;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import my.gov.perkeso.assist.core.infrastructure.data.ApiGlobalErrorResponse;
import my.gov.perkeso.assist.core.infrastructure.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

@Component
@Provider
public class ResourceNotFoundExceptionMapper implements ExceptionMapper<ResourceNotFoundException> {

    @Override
    public Response toResponse(final ResourceNotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(ApiGlobalErrorResponse.notFound(exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
