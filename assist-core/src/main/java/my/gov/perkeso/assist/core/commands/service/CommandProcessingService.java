package my.gov.perkeso.assist.core.commands.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.commands.api.JsonCommand;
import my.gov.perkeso.assist.core.commands.domain.CommandWrapper;
import my.gov.perkeso.assist.core.commands.handler.CommandHandler;
import my.gov.perkeso.assist.core.infrastructure.data.CommandProcessingResult;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommandProcessingService {

    private final CommandHandlerProvider commandHandlerProvider;
    private final ObjectMapper objectMapper;

    public CommandProcessingResult executeCommand(final CommandWrapper wrapper) {
        final JsonNode parsed = parseJson(wrapper.getJson());
        final JsonCommand command = new JsonCommand(wrapper.getJson(), parsed, wrapper.getEntityId(),
                Collections.emptyMap());
        final CommandHandler handler = commandHandlerProvider.getHandler(wrapper.getEntityName(), wrapper.getActionName());
        return handler.processCommand(command);
    }

    private JsonNode parseJson(final String json) {
        if (json == null || json.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON payload", e);
        }
    }
}
