package my.gov.perkeso.assist.core.commands.api;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import lombok.Getter;

@Getter
public class JsonCommand {

    private final String json;
    private final JsonNode parsedJson;
    private final Long entityId;
    private final Map<String, Object> commandParams;

    public JsonCommand(final String json, final JsonNode parsedJson, final Long entityId,
            final Map<String, Object> commandParams) {
        this.json = json;
        this.parsedJson = parsedJson;
        this.entityId = entityId;
        this.commandParams = commandParams;
    }

    public String stringValueOfParameterNamed(final String name) {
        if (parsedJson == null || !parsedJson.has(name) || parsedJson.get(name).isNull()) {
            return null;
        }
        return parsedJson.get(name).asText();
    }

    public Long longValueOfParameterNamed(final String name) {
        if (parsedJson == null || !parsedJson.has(name) || parsedJson.get(name).isNull()) {
            return null;
        }
        return parsedJson.get(name).asLong();
    }

    public Boolean booleanValueOfParameterNamed(final String name) {
        if (parsedJson == null || !parsedJson.has(name) || parsedJson.get(name).isNull()) {
            return null;
        }
        return parsedJson.get(name).asBoolean();
    }
}
