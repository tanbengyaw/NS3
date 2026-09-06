package my.gov.perkeso.assist.core.infrastructure.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class ApiGlobalErrorResponse {

    private final String userMessageGlobalisationCode;
    private final String defaultUserMessage;

    private ApiGlobalErrorResponse(final String userMessageGlobalisationCode, final String defaultUserMessage) {
        this.userMessageGlobalisationCode = userMessageGlobalisationCode;
        this.defaultUserMessage = defaultUserMessage;
    }

    public static ApiGlobalErrorResponse notFound(final String message) {
        return new ApiGlobalErrorResponse("error.msg.resource.not.found", message);
    }

    public static ApiGlobalErrorResponse badRequest(final String code, final String message) {
        return new ApiGlobalErrorResponse(code, message);
    }

    public static ApiGlobalErrorResponse forbidden(final String message) {
        return new ApiGlobalErrorResponse("error.msg.forbidden", message);
    }

    public static ApiGlobalErrorResponse internalError(final String message) {
        return new ApiGlobalErrorResponse("error.msg.internal", message);
    }

    @JsonProperty("userMessageGlobalisationCode")
    public String getUserMessageGlobalisationCode() {
        return userMessageGlobalisationCode;
    }

    @JsonProperty("defaultUserMessage")
    public String getDefaultUserMessage() {
        return defaultUserMessage;
    }
}
