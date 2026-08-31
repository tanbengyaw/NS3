package my.gov.perkeso.assist.core.commands.handler;

import my.gov.perkeso.assist.core.commands.api.JsonCommand;
import my.gov.perkeso.assist.core.infrastructure.data.CommandProcessingResult;

public interface CommandHandler {

    CommandProcessingResult processCommand(JsonCommand command);
}
